# jpipe-compiler

The compilation pipeline: transforms a `.jd` source file into an output
artefact through a typed, composable chain of steps. Exposes a single
`Compiler` interface to callers.

## Design

The pipeline is built on a typed DSL of four roles — `Source`, `Transformation`,
`Checker`, `Sink` — assembled via a fluent `ChainBuilder` and driven by a
`CompilationContext` threaded through every step (ADR-0009). `CompilerFactory`
wires two reusable sub-chains (`parsingChain()` and `unitBuilder()`) plus
format-specific export tails.

- [docs/design/compiler.md](../docs/design/compiler.md) — the abstract pipeline framework
- [docs/design/steps.md](../docs/design/steps.md) — every concrete step, with inputs, outputs, and behaviour

## Dependencies

- `jpipe-lang` — for the ANTLR-generated lexer and parser.
- `jpipe-model` — for `Unit`, `JustificationModel`, commands, and visitor-based exporters.
- `jpipe-operators` — for `OperatorRegistry`, `ApplyOperator`, and `LoadResolver` support.

## Build & test

```bash
mvn test -pl jpipe-compiler
```

End-to-end scenarios are written in Cucumber and live under
`src/test/resources/features/` (ADR-0011).

## Extension points

- **New pipeline step** — implement `Transformation<I,O>` or `Checker<I>` under
  `compiler.steps`, wire it into the appropriate chain in `CompilerFactory`.
- **New output format** — add a value to `Format`, implement an export
  `Transformation`, and add the corresponding `case` in `CompilerFactory.build()`.
