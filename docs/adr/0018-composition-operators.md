# ADR-0018: Composition Operators

**Date:** 2026-04-06
**Status:** Accepted

## Context

jPipe files can declare a justification (or template) as the result of composing
two or more existing models via a *composition operator*:

```
justification refined is refine(minimal, refinement) {
    hook: "minimal/e"
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
- **`SourcedElement`** — a 3-component record `(element, source, location)`
  pairing a `JustificationElement` with the `JustificationModel` it was taken
  from and its original `SourceLocation`.  The location is looked up from the
  compilation unit's location registry when `apply()` builds the sourced-element
  list, so `MergeFunction` implementations can forward it to creation commands
  and have it recorded in the result model's symbol-table entry.
- **`CompositionOperator.apply()`** — the sealed template method.  Four overloads
  are provided for call-site convenience; all delegate to the full 5-argument
  form:
  - `apply(resultName, sources, arguments)` — no location information (unit tests).
  - `apply(resultName, sources, arguments, location)` — attaches a `SourceLocation`
    to the result model declaration but supplies no element locations.
  - `apply(resultName, sources, arguments, location, knownLocations)` — the
    canonical form; `knownLocations` is a `Map<String, SourceLocation>` keyed
    on `"modelName/elementId"` (the same format used by `Unit.locations()`).
    `ApplyOperator.expand()` passes `context.locations()` here so that copied
    elements carry their source file positions into the result model.
  - Phase 1: creates elements, populates `AliasRegistry`, emits `RegisterAlias`
    commands to persist aliases in the `Unit`.
  - Phase 2: reconstructs support edges by translating original endpoints through
    the registry, with set-based deduplication to prevent duplicate edges when
    multiple source models share the same edge.
  - Subclasses provide `equivalenceRelation(sources, arguments)`,
    `mergeFunction(sources, arguments)`, `createResultModel(name, location, arguments)`,
    and optionally `requiredArguments()` (missing required keys throw
    `InvalidOperatorCallException`).  Both hook methods receive the full
    `sources` list so that operators can distinguish elements by their origin
    model without breaking the sealed `apply()`.
- **`ModelReplicator`** — a stateless utility that generates commands to copy a
  model's elements and edges non-destructively, following the same qualified-id
  convention as `JustificationModel.inline()`.
- **`OperatorRegistry`** — a name → `CompositionOperator` map populated at
  compiler startup.  Currently populated with hardcoded built-in operators in
  `CompilerFactory.builtInOperators()`; a service-loader extension point is
  deferred to a later ADR.
- **Aliases in `Unit`** — `Unit` gains `recordAlias(model, oldId, newId)`,
  `resolveAlias(model, id)`, and `aliases()` (an unmodifiable view of the full
  alias map, keyed on `"model/oldId"`), backed by a flat `Map<String, String>`.
  A new `RegisterAlias` command writes alias entries so the mapping survives in
  the compiled unit.  `aliases()` is used by `DiagnosticReport` to include
  alias entries in the symbol table.

### 2. `ApplyOperator implements MacroCommand` (`jpipe-operators`)

When the parser produces a justification or template with `ctx.operator != null`,
`ActionListProvider` emits an `ApplyOperator` command instead of
`CreateJustification` / `CreateTemplate`.

`ApplyOperator` implements the `MacroCommand` interface already used by the engine
for deferred expansion:

- `condition()` — defers until all source model names are present in the `Unit`.
- `expand(Unit)` — looks up the operator by name in the `OperatorRegistry`,
  gathers source models from the unit, and delegates to the 5-argument
  `operator.apply()`, passing the stored `SourceLocation` and `context.locations()`
  so that element locations are threaded through to the result model.
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

### 4. Symbol table for operator-created models

`DiagnosticReport` builds the symbol table by iterating `unit.getModels()`
directly rather than only the recorded-location registry.  For each element:

- If `unit.locationOf(modelName, elementId)` returns a known location (threaded
  from the source model via `knownLocations`), that location is displayed.
- Otherwise the element is marked `[synthesized]` — indicating it was created by
  the operator with no corresponding source position (e.g. a merged SubConclusion).

Aliases are shown after the element list for each model, formatted as
`oldId → newId  [alias]`.

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
- Passing `knownLocations` as a `Map<String, SourceLocation>` (rather than `Unit`
  itself) to `apply()` keeps the operator framework decoupled from the full
  compilation unit and limits the operator's read access to location data only.
- Building the symbol table from `unit.getModels()` rather than from
  `unit.locations()` ensures that operator-created models — which may have no
  element-level location entries — still appear with their complete element list.

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
- Elements copied from source models appear in the result model's symbol table
  with their original source positions.  Truly synthesized elements (e.g. a
  merged SubConclusion produced by the `refine` operator) appear as
  `[synthesized]`.  Aliases (old id → merged id) are shown separately per model.
