# ADR-0018: Composition Operators

**Date:** 2026-04-06
**Status:** Accepted

## Context

jPipe files can declare a justification (or template) as the result of composing
two or more existing models via a *composition operator*:

```
justification refined is refine(minimal, refinement) {
    hook: "justification/e"
}
```

The grammar (`JPipe.g4`) already supports this syntax. The compiler must:

1. Dispatch the operator call to the correct implementation at model-build time.
2. Remain non-destructive — source models must not be mutated.
3. Be extensible — adding a new operator must not require changes to the
   compilation pipeline.
4. Preserve the original element names in the compiled `Unit` so that
   post-composition lookups can resolve them even after elements are renamed
   during merging.

## Decision

### 1. Match-and-merge framework (`jpipe-operators` module)

Composition is modelled as a two-phase *match-and-merge* algorithm owned by
an abstract `CompositionOperator` class (Template Method pattern):

- **`EquivalenceRelation`** — a binary predicate `(a, b) → boolean` that decides
  whether two `SourcedElement`s belong to the same equivalence class.
  `CompositionOperator` partitions all source elements automatically using an
  O(n²) representative-based scan; callers need only supply the predicate.
- **`MergeFunction`** — receives one equivalence class (`ElementGroup`) and
  creates the merged element via `Command` objects.  It must register old ids →
  new id in the supplied `AliasRegistry` and must not emit `AddSupport` commands
  (edge reconstruction is automatic).
- **`CompositionOperator.apply()`** — the sealed template method:
  - Phase 1: creates elements, populates `AliasRegistry`, emits `RegisterAlias`
    commands to persist aliases in the `Unit`.
  - Phase 2: reconstructs support edges by translating original endpoints through
    the registry, with set-based deduplication to prevent duplicate edges when
    multiple source models share the same edge.
  - Subclasses provide `equivalenceRelation()`, `mergeFunction()`,
    `createResultModel()`, and optionally `requiredArguments()` (missing required
    keys throw `InvalidOperatorCallException`).
- **`ModelReplicator`** — a stateless utility that generates commands to copy a
  model's elements and edges non-destructively, following the same qualified-id
  convention as `JustificationModel.inline()`.
- **`OperatorRegistry`** — a name → `CompositionOperator` map populated at
  compiler startup.  Currently populated with hardcoded built-in operators in
  `CompilerFactory.builtInOperators()`; a service-loader extension point is
  deferred to a later ADR.
- **Aliases in `Unit`** — `Unit` gains `recordAlias(model, oldId, newId)` and
  `resolveAlias(model, id)` backed by a flat `Map<String, String>` keyed on
  `model/oldId`.  A new `RegisterAlias` command writes alias entries so the
  mapping survives in the compiled unit.

### 2. `ApplyOperator implements MacroCommand` (`jpipe-operators`)

When the parser produces a justification or template with `ctx.operator != null`,
`ActionListProvider` emits an `ApplyOperator` command instead of
`CreateJustification` / `CreateTemplate`.

`ApplyOperator` implements the `MacroCommand` interface already used by the engine
for deferred expansion:

- `condition()` — defers until all source model names are present in the `Unit`.
- `expand(Unit)` — looks up the operator by name in the `OperatorRegistry`,
  gathers source models from the unit, and delegates to `operator.apply()`.
  Returns the resulting `List<Command>` for the engine to splice at the front
  of the queue.

### 3. Compiler integration (`jpipe-compiler`)

- `ActionListProvider` receives an `OperatorRegistry` at construction
  (field named `operators`).
- In `enterJustification` / `enterTemplate`, when `ctx.operator != null`, all
  data needed for `ApplyOperator` (result name, operator name, source names,
  config map) is read eagerly from the already-built parse tree context.  The
  method returns early without updating `buildContext`, since there is no body
  to parse.
- `enterRule_config` becomes a no-op (config was consumed in the parent callback).
- `CompilerFactory.parsingChain()` passes `builtInOperators()` to
  `ActionListProvider`.

## Rationale

- Encoding the operator call as a `MacroCommand` reuses the engine's existing
  deferred-execution mechanism (also used by `ImplementsTemplate`) without
  introducing new scheduling logic.
- Collecting `rule_config` eagerly in `enterJustification` avoids accumulating
  state across child callbacks and is safe because the ANTLR parse tree is
  fully built before the walker fires any listener method.
- Separating the equivalence predicate from the merge function makes both
  independently testable and reusable across different operator implementations.
- `OperatorRegistry` as an explicit constructor dependency of `ActionListProvider`
  (rather than a static singleton) makes the available operator set visible at
  construction time and mockable in tests.
- Persisting aliases to `Unit` via `RegisterAlias` commands keeps the alias data
  in the same place as all other compiled model state, consistent with how
  `recordLocation` / `locationOf` track source positions.

## Consequences

- Adding a new built-in operator requires only implementing `CompositionOperator`
  and registering it in `CompilerFactory.builtInOperators()`.
- `ApplyOperator` is the only command that holds a reference to `OperatorRegistry`;
  all other commands remain independent of the operator framework.
- Future service-loader extensibility can be added inside `builtInOperators()`
  without changing any other class.
- Merge functions that accidentally emit `AddSupport` violate the `MergeFunction`
  contract and will produce duplicate edges; this is documented in the contract
  Javadoc but not enforced at runtime.
- An operator call with an unknown operator name causes `expand()` to throw
  `InvalidOperatorCallException`.  The engine wraps this in a `CompilationException`
  via the standard `fire()` / exception-wrapping mechanism.
