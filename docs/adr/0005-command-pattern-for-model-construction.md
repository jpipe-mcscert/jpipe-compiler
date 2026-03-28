# ADR-0005: Command Pattern for Model Construction and Modification

**Date:** 2026-03-25
**Status:** Accepted

## Context

The jPipe compiler builds a `Unit` incrementally as it parses a source file: justification models and their elements are created one by one, potentially referencing other models that may not exist yet at the point of parsing. A direct, call-by-call approach to model construction couples the parser tightly to the model API and makes deferred creation (e.g., linking to a model defined later in the file, or in another file) hard to express.

Two alternatives were considered:

- **Builder pattern** — a fluent builder per model type. Straightforward for simple cases but does not naturally support deferred execution or composition of construction steps across files.
- **Command pattern** — construction steps are first-class objects that can be collected, reordered, and executed by an engine. Supports conditional and deferred execution natively.

## Decision

The model is constructed and modified exclusively through commands (`ca.mcscert.jpipe.commands`). One command kind is defined:

- `RegularCommand` — an atomic operation executed directly on a `Unit` (e.g., creating an element, adding a support edge, inlining a template).

An `ExecutionEngine` drives execution: it processes commands sequentially and defers commands whose `condition` is not yet satisfied to the end of the queue.

## Rationale

- Deferred execution handles forward references and cross-file dependencies without requiring a multi-pass parser.
- Commands are plain objects — they are easy to log, test, and inspect independently of the model.
- The `condition` mechanism handles ordering constraints without requiring the caller to know evaluation order.
- A `MacroCommand` composite was considered for expanding multi-file `load` directives, but template inlining (ADR-0012) covers the main composition use case without requiring macro expansion. `load` is deferred; `MacroCommand` is not needed.

## Consequences

- The model API (`Unit`, `JustificationModel`) must expose mutation methods (`add`, `addInto`) callable by commands.
- Direct mutation of the model outside of commands is discouraged; the `ExecutionEngine` is the intended entry point for all model construction.
- `CreateRelation` (support edges between elements) is not yet implemented; it requires edges to be added to the element model first.
