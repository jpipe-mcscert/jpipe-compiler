# ADR-0005: Command Pattern for Model Construction and Modification

**Date:** 2026-03-25
**Status:** Accepted

## Context

The jPipe compiler builds a `Unit` incrementally as it parses a source file: justification models and their elements are created one by one, potentially referencing other models that may not exist yet at the point of parsing. A direct, call-by-call approach to model construction couples the parser tightly to the model API and makes deferred creation (e.g., linking to a model defined later in the file, or in another file) hard to express.

Two alternatives were considered:

- **Builder pattern** — a fluent builder per model type. Straightforward for simple cases but does not naturally support deferred execution or composition of construction steps across files.
- **Command pattern** — construction steps are first-class objects that can be collected, reordered, and executed by an engine. Supports conditional and deferred execution natively.

## Decision

The model is constructed and modified exclusively through commands (`ca.mcscert.jpipe.commands`). Two command kinds are defined:

- `RegularCommand` — an atomic operation executed directly on a `Unit` (e.g., creating an element, adding a support edge, inlining a template).
- `MacroCommand` — a composite that expands into a list of `RegularCommand`s at execution time. `ExecutionEngine` splices the expanded list in place and continues processing.

An `ExecutionEngine` drives execution: it processes commands sequentially, expands macros in place, and defers commands that are not ready to the end of the queue.

A command is ready when its `condition` holds on the unit built so far. A command whose outcome depends on a model being *complete* also waits for the pending commands that still change that model: each command names the model it creates or changes (`target()`), and `ready(unit, pending)` sees the rest of the queue. `implements` waits for its template, a relation for the declarations of its own model, an operator call for its source models. The meaning of a source therefore does not depend on the order of its declarations, only on what they declare.

When nothing in the queue is ready, every command waiting on another, the engine runs, best effort, a command that may run without waiting (`runWhenStuck(unit, pending)`: by default, when its `condition` holds), then resumes. It picks the first one that reads (`reads()`) no model another of them may still change, or else the first: an operator call then composes a source only once that source's own statements have run. A valid source never gets there; in an invalid one, a mistake holds back only what truly depends on it. `implements` never runs that way, so a model never inherits half a template, and an operator call never composes a source still waiting on its `implements`. When no command may run, execution deadlocks, and the stuck commands are diagnosed. The engine indexes the pending commands by the model they target, so that a readiness check looks at that model's commands only.

## Rationale

- Deferred execution handles forward references and cross-file dependencies without requiring a multi-pass parser.
- Commands are plain objects — they are easy to log, test, and inspect independently of the model.
- The `condition` and `ready` mechanisms handle ordering constraints without requiring the caller to know evaluation order.
- `MacroCommand` keeps high-level operations (e.g., overriding an abstract support) expressed as a single intent while delegating the atomic steps to independently testable `RegularCommand`s. The first concrete use case is `OverrideAbstractSupport`, which expands into `RemoveElement` + `AddElement` + `RewireStrategySupport`.

## Consequences

- The model API (`Unit`, `JustificationModel`) must expose mutation methods (`add`, `addInto`, `removeFrom`) callable by commands.
- Direct mutation of the model outside of commands is discouraged; the `ExecutionEngine` is the intended entry point for all model construction.
- `MacroCommand.expand(Unit)` is called only when the command's `condition` is satisfied; the expanded commands inherit no automatic deferral and must encode their own preconditions if needed.
