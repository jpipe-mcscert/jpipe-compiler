# ADR-0009: Pipeline Abstraction for Compilation

**Date:** 2026-03-26
**Status:** Accepted

## Context

The jPipe compiler transforms a source `.jd` file into an output artefact through
several distinct phases: file I/O, lexing, parsing, model construction, validation,
and export. These phases must be composable (sub-pipelines can be reused across
compilation modes) and extensible (new passes are added without touching existing
ones).

Three structural approaches were considered:

- **Monolithic compiler class** — a single class owns every phase as private
  methods. Simple to write initially, but phases cannot be reused independently,
  error handling is ad-hoc, and adding a new output mode requires forking the
  entire control flow.
- **`java.util.Function` composition** — phases are `Function<I, O>` values
  composed with `andThen`. Clean for pure functions, but `Function` cannot declare
  checked exceptions, carries no compilation context, and provides no hook for
  uniform logging or error wrapping.
- **Source / Transformation / Sink pipeline with explicit context** — phases are
  typed objects composable via a fluent DSL. A shared `CompilationContext` is
  threaded through every step, carrying the source path and a diagnostic bag that
  steps can write to without aborting the pipeline immediately.

## Decision

The compiler is structured as a typed pipeline of three roles:

- **`Source<I>`** — reads an input file and produces the initial pipeline value.
- **`Transformation<I, O>`** — transforms a value of type `I` into a value of
  type `O`. Steps implement the protected `run` method; callers always go through
  the final `fire` method, which enforces logging, null-output detection, fatal-error
  fast-fail, and uniform wrapping of checked exceptions into `CompilationException`.
- **`Sink<O>`** — serialises the final pipeline value to an output file.

A `Checker<I>` subclass of `Transformation<I, I>` enforces idempotency: its `run`
is sealed to always return the input unchanged; subclasses implement `check`, which
may accumulate non-fatal diagnostics via the context without throwing.

Pipelines are assembled through a fluent `ChainBuilder<I, O>` DSL, initiated by
`Source.andThen(Transformation)` and finalised by `ChainBuilder.andThen(Sink)`,
which produces a `ChainCompiler<I, O>` implementing the `Compiler` interface. A
`CompilationContext` object, created by `ChainCompiler` at the start of each
`compile` call, is threaded through every `fire` and `run` invocation. It carries
the source file path and a `Diagnostic` bag (with `WARNING`, `ERROR`, and `FATAL`
severity levels) that steps may append to. `fire` fast-fails before invoking `run`
if the context already holds a fatal diagnostic.

All three abstract types (`Source`, `Transformation`, `Checker`) expose a static
`of` / `checking` factory accepting a functional interface, so lightweight steps
can be expressed as lambdas without subclassing.

## Rationale

- The `Source` / `Transformation` / `Sink` split makes each phase independently
  testable and reusable. Sub-pipelines can be extracted as `Transformation` values
  via `ChainBuilder.asTransformation()` and embedded in larger pipelines without
  wrapping.
- The Template Method pairing of `run` (extension point) and `fire` (sealed public
  entry) ensures that logging, null checks, and error handling cannot be bypassed
  by a step implementation. `Checker` applies the same pattern one level down to
  guarantee idempotency by construction.
- Threading `CompilationContext` through every call, rather than using a shared
  mutable singleton or passing a plain `String` source path, makes the diagnostic
  bag visible at every call site without hidden state. Steps that want to report
  multiple errors do so via `ctx.error()`; only when the pipeline reaches a
  `Checker` or checkpoint step that calls `ctx.fatal()` does execution stop.
- `CompilationException` (unchecked) replaces the previous `ErrorManager`
  singleton, removing a global mutable dependency and making failure modes
  explicit in the call graph.
- The `of` / `checking` factories avoid requiring a full subclass for one-off or
  test-only steps, keeping the DSL as concise as `Function` composition while
  retaining the richer contract.

## Consequences

- Every new compilation phase is a subclass (or `of`-constructed instance) of
  `Transformation` or `Checker`, placed under `compiler.steps`. No phase may
  mutate shared state outside of the `CompilationContext` it receives.
- `ChainCompiler` creates one `CompilationContext` per `compile` call; the context
  is not thread-safe and must not be shared across concurrent compilations.
- Steps that need to report multiple errors before aborting should use
  `ctx.error()` for each and reserve `ctx.fatal()` (or throwing) for conditions
  that make further progress meaningless. A dedicated `Checkpoint` step pattern
  may be introduced to drain non-fatal errors into a halt at a defined boundary.
- `Compiler` remains a simple two-argument interface (`sourceFile`, `sinkFile`).
  Callers that need in-memory compilation (e.g., tests or embedding) can supply a
  `Source` that ignores the path argument and a `Sink` that writes to a buffer.
