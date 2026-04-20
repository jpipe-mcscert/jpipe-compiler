# jpipe-operators

Composition operators: the mechanism by which new justification models are
assembled from existing ones. Defines the operator framework, the built-in
`refine` and `assemble` operators, and the post-composition unification pass.

## Design

All operators extend `CompositionOperator`, which owns a sealed four-phase
`apply()` template method (partition → create → link → additional). The
`ApplyOperator` macro command drives execution at model-build time.
Post-composition unification is handled by `Unifier`. See
[docs/design/operators.md](../docs/design/operators.md) for full type diagrams,
phase descriptions, and step-by-step extension guides. Introduced in ADR-0018.

## Dependencies

- `jpipe-model` — for `JustificationModel`, `Command`, `MacroCommand`,
  `ExecutionEngine`, and all element types.

## Build & test

```bash
mvn test -pl jpipe-operators
```

## Extension points

- **New operator** — extend `CompositionOperator` in `operators.builtin`,
  register it in `CompilerFactory.builtInOperators()`.
- **New equivalence relation** — implement `EquivalenceRelation` in
  `operators.equivalences`, register it in
  `CompilerFactory.builtInUnificationEquivalences()` for use in `unifyBy`.

See [docs/design/operators.md](../docs/design/operators.md) for worked
examples of both.
