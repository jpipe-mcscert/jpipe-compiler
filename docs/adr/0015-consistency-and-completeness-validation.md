# ADR-0015: Consistency and Completeness Validation

**Date:** 2026-03-29
**Status:** Accepted

## Context

A compiled jPipe model can be structurally well-formed in two independent senses:

- **Consistent** — the support graph satisfies its structural invariants: no element
  appears twice under the same model, and the graph contains no cycles.
- **Complete** — every required element is present and every node in the support
  graph has the required connections: a justification has a conclusion, every
  strategy has at least one supporting leaf, and so on.

These checks were previously conflated inside `JustificationModel.lock()`. That
method validated completeness and simultaneously marked the model immutable. It was
never called in the compiler pipeline — only in tests — so neither validation ran
during normal compilation. Violations produced exceptions rather than structured
diagnostics, with no source location attached.

Three design questions arose:

1. **Where do the rules live?** In the domain model (`jpipe-model`) or in the
   compiler pipeline (`jpipe-compiler`)?
2. **How are violations structured?** Exceptions, plain strings, or typed records?
3. **Is the `lock()` mechanism still needed?**

## Decision

### Rules live in `jpipe-model`; the compiler is a thin wrapper

Consistency and completeness rules are pure domain logic. They operate entirely on
model types (`Unit`, `JustificationModel`, and its elements) and carry no dependency
on pipeline types (`CompilationContext`, `Diagnostic`, etc.).

Following ADR-0010, they are implemented as **validators** in
`ca.mcscert.jpipe.model.validation`:

- `ConsistencyValidator` — structural invariants.
- `CompletenessValidator` — presence and connectivity requirements.

Each exposes two entry points:

```java
List<Violation> validate(Unit unit)          // location-aware; used by the pipeline
List<Violation> validateModel(JustificationModel<?> model)  // standalone; used by JustificationModel.validate()
```

`validate(Unit)` resolves source locations from `Unit`'s location registry
(`unit.locationOf(modelName, elementId)`) so that violations carry a precise
`SourceLocation`. `validateModel` produces `SourceLocation.UNKNOWN` for all
violations because no location data is available outside a compilation context.

Two thin `Checker<Unit>` steps in `jpipe-compiler` (`ConsistencyChecker`,
`CompletenessChecker`) invoke the validators and map each `Violation` to a
`ctx.error()` call, attaching the resolved location when it is known.

### Violations are typed records

Violations are represented by `ca.mcscert.jpipe.model.Violation`:

```java
public record Violation(String rule, String message, SourceLocation location) {}
```

`rule` is a stable kebab-case identifier that callers can use for filtering or
localisation without parsing the human-readable `message`. `location` is the
source position of the offending element, or `SourceLocation.UNKNOWN`.

### `lock()` is removed

`lock()` conflated two concerns: completeness validation and immutability
enforcement. With rule-based validators superseding its validation logic, and given
that it was never called in production code, the mechanism is removed entirely.
Along with it: `isLocked()`, the `locked` field, the `validateForLock()` hook, the
immutability guard in `addElement()`, `IncompleteJustificationException`, and
`LockedModelException`.

`JustificationModel.validate()` is provided as a convenience for non-compiler
consumers that build models programmatically and want validation without a full
pipeline. It delegates to both validators and returns an unmodifiable list of
violations (all with `SourceLocation.UNKNOWN`).

### Pipeline placement

> _The `HaltAndCatchFire` checkpoints between the checkers were removed by
> ADR-0016. Consistency and completeness errors now accumulate without aborting
> the pipeline. The pipeline placement below reflects the original decision; see
> ADR-0016 for the current arrangement._

Consistency is checked before completeness:

```
ActionListInterpretation
  → ConsistencyChecker
  → CompletenessChecker
```

Consistency first: a structurally broken model (cycles, duplicate IDs) can produce
misleading completeness results.

## Consistency rules

| Rule | Description |
|------|-------------|
| `no-duplicate-ids` | All element IDs within a model are unique, including the conclusion's ID. |
| `acyclic-support` | The support graph is acyclic. A cycle is detected when following support edges from any node visits a node already on the current traversal path. |

Note: type constraints (Conclusion ← Strategy ← SupportLeaf only) are enforced by
the Java type system at construction time and need no runtime rule.

## Completeness rules

Rules marked **J** apply to `Justification` only; **T** to `Template` only; all
others apply to both.

| Rule | Scope | Description |
|------|-------|-------------|
| `conclusion-present` | J + T | The model has a `Conclusion`. |
| `conclusion-supported` | J + T | The conclusion has an assigned `Strategy`. |
| `strategy-supported` | J + T | Every `Strategy` has at least one supporting leaf (`Evidence`, `SubConclusion`, or `AbstractSupport`). |
| `sub-conclusion-supported` | J + T | Every `SubConclusion` has an assigned `Strategy`. |
| `no-abstract-support` | J | No `AbstractSupport` placeholder remains; all must have been replaced by override commands before the completeness check runs. |
| `has-abstract-support` | T | The template declares at least one `AbstractSupport`; a template with none is a justification in disguise. |

## Rationale

- **Separation of concerns.** Validation logic belongs to the domain. The compiler
  step is glue, not logic. This keeps both independently testable and keeps
  `jpipe-model` usable without the compiler.
- **Source traceability.** Starting validation from `Unit` (which owns the location
  registry) rather than from individual `JustificationModel` instances means every
  violation can carry a precise file/line/column without a separate enrichment step.
  Violations are location-rich by construction in pipeline mode.
- **Structured violations.** A typed `Violation` record with a stable `rule` field
  is more useful than an exception or a bare string list. Downstream tools can
  filter by rule name; the compiler maps the same records to `Diagnostic` entries.
- **Removing `lock()`.** Immutability was never enforced at runtime (lock() was
  not called in the pipeline). The validation half is superseded by the new
  validators. Removing the mechanism reduces API surface and eliminates a source
  of confusion about model lifecycle.

## Consequences

- `ConsistencyValidator` and `CompletenessValidator` in `jpipe-model` are the
  canonical home for new validation rules. To add a rule: implement it in the
  appropriate validator, add a test, and the compiler picks it up automatically.
- `JustificationModel` no longer enforces immutability. Code that previously relied
  on `isLocked()` or `LockedModelException` must be updated.
- The `override.jd` example file was updated: it previously contained a justification
  with no conclusion (an incompleteness that `lock()` never caught). The fix — adding
  a conclusion and a connecting strategy — also serves as documentation of the
  expected structure for template-implementing justifications.
