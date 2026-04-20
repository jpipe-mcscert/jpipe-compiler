# ADR-0010: Model-Only Visitors Belong in `jpipe-model`

**Date:** 2026-03-27
**Status:** Accepted

## Context

`JustificationVisitor<R>` (ADR-0008) enables consumers to traverse the compiled
model without coupling to its internals. Several kinds of consumers implement this
interface:

- **Exporters** — serialise a model to a target format (e.g. `.jd` text, GraphViz
  DOT, JSON).
- **Validators / linters** — inspect a model and accumulate diagnostic messages.
- **Transformers** — produce a new model or derived data structure.

The question is where these implementations should live. Two placements were
considered:

- **`jpipe-compiler`** — co-locate visitor implementations with the compilation
  pipeline steps that invoke them. Convenient when a visitor is only ever used
  inside a `Transformation` step.
- **`jpipe-model`** — place visitor implementations alongside the model types and
  the `JustificationVisitor` interface they implement.

Keeping everything in `jpipe-compiler` creates an unnecessary coupling: any tool,
plugin, or test suite that wants to pretty-print or validate a model must take a
full dependency on the compiler module, even though the logic in question operates
exclusively on model types and has no compile-pipeline concerns.

## Decision

Visitor implementations that operate **solely on model types** are placed in the
`jpipe-model` module, under the `ca.mcscert.jpipe.visitor` package (alongside
`JustificationVisitor`).

A `Transformation` step in `jpipe-compiler` that wants to use such a visitor
simply instantiates it and calls its public API; the step acts as glue between the
pipeline and the visitor, not as the host of the traversal logic.

A visitor belongs in `jpipe-model` if and only if its entire implementation
references only types from `jpipe-model`. If a visitor needs pipeline types
(`CompilationContext`, `Diagnostic`, etc.) it may live in `jpipe-compiler`.

## Rationale

- A model exporter such as one that renders a `Unit` back to `.jd` syntax is
  useful anywhere a `Unit` is available — in tests, in external tools, in a
  language server — none of which should need the compiler on their classpath.
- The `jpipe-model` module already owns `JustificationVisitor` and all the types
  a model visitor references; placing implementations there keeps cohesion high
  and avoids a dependency inversion.
- The compiler step that invokes the visitor remains thin: its `run` method
  instantiates the visitor, calls its entry point, and returns the result. The
  separation makes both the visitor and the step independently testable.

## Consequences

- New exporters or inspectors that work purely on the model are added to
  `jpipe-model` (`ca.mcscert.jpipe.visitor`), not to `jpipe-compiler`.
- The corresponding `Transformation` or `Checker` step in `jpipe-compiler` is a
  one-method wrapper that delegates to the visitor.
- If a visitor grows a dependency on compiler-only types, it should be moved (or
  split) into `jpipe-compiler`. The rule is about actual dependencies, not intent.
