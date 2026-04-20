# ADR-0002: Multi-Module Structure

**Date:** 2026-03-25
**Status:** Accepted

## Context

The original jPipe compiler was a single Maven module. All concerns — grammar, domain model, operator logic, pipeline execution, and CLI — were mixed together, making it difficult to test components in isolation, extend the operator system, or reuse the compiler outside the CLI context.

## Decision

Split the compiler into five Maven modules with a strict one-way dependency chain:

```
jpipe-cli → jpipe-compiler → jpipe-model
                           ↘
               jpipe-lang ──┘
               jpipe-operators → jpipe-model
```

## Module responsibilities

### `jpipe-lang`
Contains the ANTLR4 grammar (`JPipe.g4`) and the generated lexer/parser. Nothing else. No domain knowledge, no interpretation logic. Consumers depend on this module to get a parse tree from source text.

### `jpipe-model`
Contains the domain model: justification elements (`Evidence`, `Strategy`, `Conclusion`, `SubConclusion`, `AbstractSupport`), justification models (`Justification`, `Template`), and the symbol table. No parsing, no I/O, no operator logic.

### `jpipe-operators`
Contains the `CompositionOperator` interface (the extension point for user-defined operators) and the built-in operators (`assemble`, `merge`, `refine`). Depends on `jpipe-model` to manipulate elements. External projects that define custom operators depend only on this module.

### `jpipe-compiler`
Contains the compiler pipeline: reads a parse tree from `jpipe-lang`, builds a model using `jpipe-model`, validates it, applies operators from `jpipe-operators`, and produces exportable output. No CLI concerns.

### `jpipe-cli`
Contains the command-line interface (Picocli), output rendering, and the fat JAR entry point. Depends on `jpipe-compiler`. This is the only module that pulls in runtime dependencies (Log4j2 core, org.json).

## Consequences

- Custom operator authors depend only on `jpipe-operators` — they are isolated from grammar and pipeline changes.
- The compiler pipeline can be embedded in other tools (IDE plugins, CI integrations) by depending on `jpipe-compiler` without the CLI.
- `jpipe-lang` can be regenerated at any time from the grammar without affecting the rest of the codebase.
- Module boundaries must be respected: no upward dependencies (e.g., `jpipe-model` must never import from `jpipe-compiler`).
