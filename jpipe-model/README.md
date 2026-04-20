# jpipe-model

Domain model for jPipe: the element hierarchy, justification models, the
command infrastructure for model construction, and the visitor for traversal.
Every other module builds on or reads from these types.

## Design

The element hierarchy is rooted at the sealed interface `JustificationElement`
and partitioned into `Justification` and `Template` models held in a `Unit`.
Model construction uses the **Command pattern** (ADR-0005); traversal uses the
**Visitor pattern** (ADR-0008). See [docs/design/model.md](../docs/design/model.md)
for the full type diagrams.

## Dependencies

- `jpipe-lang` — for `SourceLocation` and grammar-generated token types used
  in command constructors.

## Build & test

```bash
mvn test -pl jpipe-model
```
