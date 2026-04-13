# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Test Commands

```bash
# Build all modules
mvn package

# Run all tests
mvn test

# Run tests for a single module
mvn test -pl jpipe-model
mvn test -pl jpipe-compiler

# Run a single test class
mvn test -pl jpipe-compiler -Dtest=MyTestClass

# Verify (includes checkstyle, coverage)
mvn verify

# Apply code formatting (required before committing)
mvn spotless:apply

# Check formatting without applying
mvn spotless:check
```

Code **must** pass `mvn spotless:apply` (Google Java Format) and `mvn verify` before committing. Spotless is non-negotiable — CI will reject unformatted code.

## Module Structure

| Module | Role |
|--------|------|
| `jpipe-lang` | ANTLR4 grammar (`JPipe.g4`); generates lexer/parser |
| `jpipe-model` | Domain model: element hierarchy, commands, visitor |
| `jpipe-operators` | Composition operator extension point (placeholder) |
| `jpipe-compiler` | Compilation pipeline: source → transform → check → sink |
| `jpipe-cli` | PicoCLI entry point; fat JAR via Maven Shade |

All packages use `ca.mcscert.jpipe` as the base namespace.

## Compiler Pipeline Architecture

The compiler (`jpipe-compiler`) is built on a **typed pipeline DSL** (see ADR-0009 and `docs/design/compiler.md`):

```
Source<I> → Transformation<I,M> → ... → Checker<M> → ... → Sink<O>
```

The four roles:
- **`Source<I>`** — reads a file, produces the initial value
- **`Transformation<I,O>`** — converts one type to another; subclasses implement `protected run(I, ctx)`, callers use `final fire(I, ctx)` which handles logging, null detection, fatal-fast-fail, and exception wrapping
- **`Checker<I>`** — `Transformation<I,I>`; guaranteed not to modify its input; use for validation steps
- **`Sink<O>`** — serializes the final value to a file

Pipeline is assembled via a fluent builder:
```java
new CharStreamProvider()
    .andThen(new Lexer())
    .andThen(new Parser())
    .andThen(new HaltAndCatchFire<>())
    ...
    .andThen(sink);
```

`CompilationContext` is threaded through every step and carries the source path and a `Diagnostic` bag. Diagnostics have three severities: `WARNING`, `ERROR` (non-fatal, accumulates), `FATAL`. `fire()` skips `run()` if the context already has a fatal error. `HaltAndCatchFire` is a checkpoint that promotes accumulated `ERROR`s to `FATAL`, preventing downstream steps from working on broken intermediate state.

## Domain Model Architecture

See `docs/design/model.md`. Key sealed hierarchies:

```
JustificationElement (sealed)
├── CommonElement — appears in Justifications and Templates
│   ├── Conclusion (required, one per model)
│   ├── SubConclusion
│   ├── Strategy
│   └── Evidence (record)
└── AbstractSupport (record) — template-only placeholder

SupportLeaf (marker interface) — implemented by SubConclusion, Evidence, AbstractSupport
```

`JustificationModel<E>` (sealed abstract base):
- `Justification` — accepts only `CommonElement`; concrete justification
- `Template` — accepts any `JustificationElement`; abstract/reusable structure

Model construction uses the **Command pattern** (ADR-0005): `CreateConclusion`, `CreateStrategy`, `CreateAbstractSupport`, etc. Traversal uses the **Visitor pattern** (ADR-0008): `JustificationVisitor<R>`.

Models lock after construction; completeness validation runs before locking.

## Design Decisions (ADRs)

Architecture decisions live in `docs/adr/`. Notable ones:
- **ADR-0005** — Command pattern for model construction
- **ADR-0008** — Visitor pattern for model traversal
- **ADR-0009** — Typed pipeline abstraction (most recent, 2026-03-26)

Design rationale docs are in `docs/design/compiler.md` and `docs/design/model.md`.

## Development Conventions

- **Diagnostics:** Use `CompilationContext` to report `WARNING`, `ERROR`, or `FATAL` diagnostics. Avoid throwing raw exceptions; prefer wrapping in `CompilationException` via the pipeline's `fire()` method.
- **Pipeline steps:** New compilation phases must be implemented as `Transformation<I,O>` or `Checker<I>` subclasses and placed under `ca.mcscert.jpipe.compiler.steps`.
- **Testing:** New features must include Cucumber scenarios in `jpipe-compiler/src/test/resources/features` and JUnit Jupiter unit tests in the relevant module.

## Naming Conventions

### Element IDs and qualified ids
Element ids follow the qualified-id scheme documented in ADR-0012. A plain id (`s`) is the form
used in source `.jd` files. The compiler qualifies it with the owning model or namespace at
expansion time (`templateName:s`, `ns:model:s`). `JustificationModel.findById` resolves both
forms — callers should pass the plain id when possible and let the model handle qualification.

### Class naming
| Kind | Pattern | Examples |
|------|---------|---------|
| Model construction command | `Create<X>`, `Add<X>`, `Rewire<X>`, `Override<X>` | `CreateConclusion`, `AddSupport`, `RewireStrategySupport` |
| Compiler pipeline step | noun phrase for the step's role | `HaltAndCatchFire`, `SelectModel`, `LoadResolver` |
| Visitor / exporter | `<Format>Exporter`, `<Purpose>Visitor` | `DotExporter`, `PythonExporter`, `JsonExporter` |
| Validator | `<Concern>Validator` | `ConsistencyValidator`, `CompletenessValidator` |
| Test class | mirrors the class under test + `Test` suffix | `DotExporterTest`, `AddSupportTest` |

### Method naming in commands and pipeline steps
- Command execution entry point: `doExecute(Unit)` (called by `RegularCommand.execute`).
- Pipeline entry point: `run(I, CompilationContext)` (called by `Transformation.fire`).
- Visitor entry points: `visit(<Type>)` — one overload per node type in the hierarchy.

## Logging

Follow ADR-0006 logging conventions. Log4j 2 is the logging framework.
