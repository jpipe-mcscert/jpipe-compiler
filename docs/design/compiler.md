# Compiler

The `jpipe-compiler` module transforms a `.jd` source file into an output
artefact through a typed, composable pipeline. It is structured around two
packages.

## Packages

### `compiler`

The top-level package exposes a single public contract and its two concrete
implementations.

`Compiler` is a two-method interface: `compile(sourceFile, sinkFile)`. It is
the only type that callers outside the module need to reference.

`ChainCompiler` is the standard implementation, assembled by the fluent DSL in
`compiler.model`. It is not instantiated directly — `ChainBuilder.andThen(Sink)`
produces it as the final step of pipeline construction.

```plantuml
@startuml compiler

skinparam packageStyle rectangle
skinparam classAttributeIconSize 0
hide empty members

package "compiler" {

  interface Compiler {
    + compile(sourceFile : String, sinkFile : String) : boolean
  }

  class ChainCompiler<I, O> {
    - source : Source<I>
    - chain : Transformation<I, O>
    - sink : Sink<O>
    + compile(String, String) : boolean
  }

  Compiler <|.. ChainCompiler
}

@enduml
```

### `compiler.model`

The pipeline abstraction. Every compilation pipeline is built from three roles —
**Source**, **Transformation**, **Sink** — assembled through a fluent
`ChainBuilder` DSL and driven by a `CompilationContext` that is threaded through
every step.

**Roles**

- **`Source<I>`** — first step: reads a file path and produces the initial
  pipeline value of type `I`. Subclasses implement `provideFrom`; lightweight
  sources can be expressed as lambdas via `Source.of(Provider)`.
- **`Transformation<I, O>`** — middle step: a typed function `I → O`. Subclasses
  implement the protected `run` method; callers always go through the final
  `fire` method, which handles logging, null-output detection, fast-fail on
  accumulated fatal errors, and wrapping of checked exceptions into
  `CompilationException`. Lightweight steps can be expressed as lambdas via
  `Transformation.of(Step)`. Steps are composed via `andThen`.
- **`Checker<I>`** — a specialisation of `Transformation<I, I>` whose `run` is
  sealed to always return its input unchanged. Subclasses implement `check`,
  which may report non-fatal diagnostics via the context without throwing.
  Lightweight checkers can be expressed as lambdas via `Checker.checking(Check)`.
- **`Sink<O>`** — last step: serialises the final pipeline value to a file.
  Implemented as an interface so lambda expressions are supported directly.

**Assembly**

`Source.andThen(Transformation)` starts the builder, returning a
`ChainBuilder<I, O>`. Subsequent `andThen(Transformation)` calls extend the
chain, each returning a new immutable `ChainBuilder` with the composed type.
`ChainBuilder.andThen(Sink)` closes the chain and returns a ready-to-use
`ChainCompiler`. `ChainBuilder.asTransformation()` exposes the accumulated chain
as a plain `Transformation`, allowing sub-pipelines to be embedded inside
larger ones.

**Context**

`ChainCompiler.compile` creates one `CompilationContext` per call and threads
it through every `fire` and `run` invocation. The context carries the source
file path and a `Diagnostic` bag. Steps report issues via `ctx.error()` or
`ctx.fatal()`; `fire` fast-fails before running a step if the context already
holds a fatal diagnostic. `Diagnostic` is a record with two severity levels:
`ERROR` and `FATAL`. `compile` returns `true` when at least one `ERROR` or
`FATAL` was reported (see ADR-0016).

```plantuml
@startuml compiler-model

skinparam packageStyle rectangle
skinparam classAttributeIconSize 0
hide empty members

interface Compiler

package "compiler.model" {

  abstract class Source<I> {
    + {static} of(Provider<I>) : Source<I>
    + provideFrom(sourceName : String) : I
    + andThen(Transformation<I,R>) : ChainBuilder<I,R>
  }

  abstract class Transformation<I, O> {
    + {static} of(Step<I,O>) : Transformation<I,O>
    # run(I, CompilationContext) : O
    + fire(I, CompilationContext) : O
    + andThen(Transformation<O,R>) : Transformation<I,R>
  }

  abstract class Checker<I> {
    + {static} checking(Check<I>) : Checker<I>
    # check(I, CompilationContext) : void
  }

  interface Sink<O> {
    + pourInto(O, fileName : String) : void
  }

  class ChainBuilder<I, O> {
    + andThen(Transformation<O,R>) : ChainBuilder<I,R>
    + andThen(Sink<O>) : ChainCompiler<I,O>
    + asTransformation() : Transformation<I,O>
  }

  class ChainCompiler<I, O> {
    + compile(String, String) : boolean
  }

  class CompilationContext {
    - sourcePath : String
    - diagnostics : List<Diagnostic>
    + sourcePath() : String
    + error(String) : void
    + fatal(String) : void
    + report(Diagnostic) : void
    + hasErrors() : boolean
    + hasFatalErrors() : boolean
    + diagnostics() : List<Diagnostic>
  }

  class Diagnostic <<record>> {
    + level : Level
    + source : String
    + message : String
    + isError() : boolean
    + isFatal() : boolean
  }

  enum Level {
    ERROR
    FATAL
  }

  class CompilationException {
    + CompilationException(step : String, cause : Throwable)
    + CompilationException(step : String, reason : String)
  }

  Transformation <|-- Checker
  Compiler <|.. ChainCompiler

  Source         ..>  ChainBuilder    : creates
  ChainBuilder   ..>  ChainCompiler   : creates
  ChainBuilder   o--  Source
  ChainBuilder   o--  Transformation
  ChainCompiler  o--  Source
  ChainCompiler  o--  Transformation
  ChainCompiler  o--  Sink
  ChainCompiler  ..>  CompilationContext : creates

  Transformation ..>  CompilationContext : reads / writes
  Transformation ..>  CompilationException : throws

  CompilationContext o-- Diagnostic
  Diagnostic         +-- Level
}

@enduml
```

The sequence below shows a single `compile` call flowing through a three-step
pipeline.

```plantuml
@startuml compiler-sequence

skinparam sequenceMessageAlign left
hide footbox

participant Caller
participant ChainCompiler
participant CompilationContext
participant Source
participant "T₁ : Transformation" as T1
participant "T₂ : Transformation" as T2
participant Sink

Caller        ->  ChainCompiler      : compile(sourceFile, sinkFile)
ChainCompiler ->  CompilationContext : new(sourceFile)
ChainCompiler ->  Source             : provideFrom(sourceFile)
Source        --> ChainCompiler      : input

ChainCompiler ->  T1                 : fire(input, ctx)
T1            ->  CompilationContext : hasFatalErrors() → false
T1            ->  T1                 : run(input, ctx)
T1            --> ChainCompiler      : intermediate

ChainCompiler ->  T2                 : fire(intermediate, ctx)
T2            ->  CompilationContext : hasFatalErrors() → false
T2            ->  T2                 : run(intermediate, ctx)
T2            --> ChainCompiler      : output

ChainCompiler ->  Sink               : pourInto(output, sinkFile)

@enduml
```
