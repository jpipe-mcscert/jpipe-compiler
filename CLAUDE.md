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

All packages use `org.jpipe` group ID (was `ca.mcscert.jpipe` before ADR-0002 rename — some files still use the old namespace).

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

## Logging

Follow ADR-0006 logging conventions. Log4j 2 is the logging framework.
