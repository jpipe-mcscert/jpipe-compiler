# ADR-0016: Error Management — Diagnostic Levels, Codes, Pipeline Interruption, and Exit Codes

**Date:** 2026-04-01
**Last revised:** 2026-08-09
**Status:** Accepted

## Context

The compiler uses a typed pipeline (ADR-0009) where each step fires in sequence,
threading a `CompilationContext` that accumulates `Diagnostic` entries. Two
categories of problem can arise during compilation:

1. **Syntax errors** — the parse tree is broken. Continuing downstream steps would
   build a model from garbage and produce misleading secondary errors.
2. **Semantic errors** — the model is structurally sound but violates domain rules
   (consistency, completeness). The model can still be exported; the errors are
   informational for the user.

Diagnostics have two audiences: a person reading a terminal, and tooling that
consumes the compiler's output (IDE integrations, CI dashboards). A diagnostic
shaped only for the first forces the second to parse prose.

Prior to this ADR:
- `HaltAndCatchFire` checkpoints were placed after *both* parsing and semantic
  checking, aborting the pipeline on any accumulated error.
- `Compiler.compile()` returned `void`, so non-fatal errors accumulated silently and
  the CLI always exited 0 on a successful run — even when errors had been diagnosed.
- A `WARNING` level existed in `Diagnostic.Level` but was never emitted by any
  production step, creating an undefined level with no behavioral contract.

## Decision

### Two diagnostic levels: ERROR and FATAL

`WARNING` is removed. The two remaining levels have explicit, distinct contracts:

| Level | Pipeline effect | Exit code effect |
|-------|----------------|-----------------|
| `ERROR` | None — accumulates in `CompilationContext` | Exit 1 at end of compilation |
| `FATAL` | Aborts pipeline at the next `fire()` boundary | Exit 1 (via `CompilationException`) |

`ERROR` is the default level for diagnosed problems. It signals "something is wrong
and the user must see it", but allows the pipeline to continue and produce output.

`FATAL` means "continuing is meaningless or dangerous". It is promoted from `ERROR`
only by `HaltAndCatchFire`, and only in contexts where downstream steps cannot
function correctly on the resulting intermediate value.

### Diagnostics carry a machine-readable code

A `Diagnostic` is a level, an optional code, a source location, and a
human-readable message. The code is a stable, bare kebab-case identifier —
`unknown-model`, `no-duplicate-ids` — or `null` when the diagnostic has none;
`hasCode()` reports its presence.

Compiler codes are centralised in `DiagnosticCodes`. Validation rules reach the
same field through `Violation.rule()` (ADR-0015), which is already bare
kebab-case, so the two families are directly comparable and no translation layer
sits between a validator and a diagnostic.

The code is **data, not text**: it never appears inside `message()`, and each
renderer chooses its own presentation.

| Renderer | Presentation |
|----------|--------------|
| Human-readable diagnostic report | `[unknown-model] unknown model 'foo'` |
| `ChainCompiler` stderr fallback | `[unknown-model] unknown model 'foo'` |
| JSON diagnostic report | a separate `code` member |

Codes are optional by design. Summary diagnostics such as `"model construction
failed — see errors above"` name no specific rule, and none is invented for
them. `FATAL` diagnostics carry no code either: a fatal names a
compilation-level failure — the pipeline cannot continue — rather than a rule
that was broken.

### HaltAndCatchFire is placed exclusively after parsing

`HaltAndCatchFire` converts all accumulated `ERROR` diagnostics into a single
`FATAL`, which causes the next `Transformation.fire()` call to throw
`CompilationException` and skip remaining steps.

This checkpoint is placed **after parsing** only:

```
FileSource → CharStreamProvider → Lexer → Parser → HaltAndCatchFire → ActionListProvider → ...
```

Rationale: a broken parse tree makes subsequent model-construction steps produce
incorrect or misleading results. Aborting early prevents cascading phantom errors.

Semantic checkers (`ConsistencyChecker`, `CompletenessChecker`) run **without** a
following `HaltAndCatchFire`. Their errors accumulate and the pipeline continues to
export. A structurally broken model (invalid parse) is fundamentally different from a
semantically incomplete one (missing conclusion): the latter is still a valid Java
object that can be serialised and inspected.

### compile() returns boolean hasErrors

`Compiler.compile()` is changed from `void` to `boolean`, returning `true` when at
least one `ERROR` or `FATAL` diagnostic was reported. This allows the CLI entry
points to inspect the outcome without access to `CompilationContext`.

`ChainCompiler` returns `ctx.hasErrors()` after the pipeline completes. If the
pipeline throws (e.g. due to a `FATAL`), the exception propagates naturally and the
return value is never reached — the CLI catch blocks handle that path.

### Reporting on a compilation that aborted

The pipeline invariant above is absolute: no step runs after a `FATAL`. That
makes a report assembled as the last steps of a pipeline unable to describe the
very failures that stopped it — which is what the `diagnostic` command exists to
do.

The command therefore has its own `DiagnosticCompiler`, which runs the analysis
as a pipeline and then writes the report whether that pipeline completed or
aborted; when it aborted, on the empty unit it never got to build. Rendering is
reachable outside `fire()` for that reason (`CollectDiagnostics.snapshot` and
`DiagnosticRenderer.render`). The exit code is unchanged: `hasErrors()` counts a
`FATAL`, so the command still exits 1.

`process` keeps the original behaviour, because its output stream carries a
model export and a report written into it would corrupt the artefact.

### CLI exit codes

| Situation | Exit code |
|-----------|-----------|
| Compilation succeeded, no errors | 0 |
| Compilation succeeded, errors diagnosed | 1 |
| Pipeline aborted (syntax errors or explicit FATAL) | 1 |
| Unexpected exception (system error) | 42 |

Exit code 42 is reserved for exceptions that escape all `CompilationException` and
`UnsupportedOperationException` handlers — i.e., bugs or environmental failures
outside the compiler's control.

## Rationale

- **Syntax vs. semantic distinction.** Syntax errors invalidate the parse tree; no
  useful work can follow. Semantic errors are domain-level feedback; export is still
  meaningful and the user benefits from seeing the output alongside the diagnostics.
- **Removing WARNING.** A level with no behavioral contract is worse than no level:
  it creates ambiguity about whether a warning affects the exit code, whether it
  interrupts the pipeline, and whether it should be printed. With only two levels,
  the contract is unambiguous.
- **boolean return over exception.** Throwing a post-completion exception to signal
  "succeeded with errors" would cause the CLI to print a redundant error message on
  top of already-printed diagnostics. A boolean return keeps the separation clean.
- **Codes as a field, not a message prefix.** Encoding the code inside the message
  would make every consumer re-parse a string the compiler had just assembled, and
  would discard at the checker boundary the identifier the validators already carry.
  One representation, chosen by each renderer at display time, keeps the human and
  machine readings of a diagnostic from drifting apart.

## Consequences

- `Diagnostic.Level` has two values: `ERROR` and `FATAL`. Any future addition of a
  new level (e.g., `INFO`) must define its pipeline and exit-code contract in a new
  ADR before being added.
- `CompilationContext.warn()` is removed. Steps that previously emitted warnings
  should either promote to `error()` or omit the diagnostic entirely.
- The pipeline placement section of ADR-0015 is superseded by this ADR (see note
  there).
- All CLI commands must check the boolean return of `compile()` and map `true` to
  `EXIT_JPIPE_ERROR`.
- `Diagnostic.message()` never contains the code; consumers read `code()`. Tests and
  tooling match on the code directly rather than searching the message text.
- Introducing a diagnostic code means adding a constant to `DiagnosticCodes`, or
  naming a rule in a validator — not editing a message string. A renderer that wants
  to display it decides how.
- Any renderer of diagnostics must handle a `null` code, since the field is optional.
