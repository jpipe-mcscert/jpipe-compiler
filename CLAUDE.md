# CLAUDE.md

This file provides guidance to Claude Code when working with this repository.

---

## Project Overview

**jPipe (Justified Pipelines)** is a compiler and language environment for
defining justifications that support software maintenance and CI/CD activities.

- **Language:** Java 25
- **Build system:** Maven (multi-module)
- **Key libraries:** ANTLR4 (grammar/parsing), PicoCLI (CLI), Cucumber (E2E
  tests), JUnit 5 + AssertJ (unit tests), Log4j 2 (logging)
- **Core patterns:** Typed pipeline DSL, Command pattern (model construction),
  Visitor pattern (model traversal), sealed type hierarchies

---

## Module Structure

| Module | Role |
|--------|------|
| `jpipe-lang` | ANTLR4 grammar (`JPipe.g4`); generates lexer/parser |
| `jpipe-model` | Domain model: element hierarchy, commands, visitor |
| `jpipe-operators` | Composition operators (`refine`, `assemble`) and extension point |
| `jpipe-compiler` | Compilation pipeline: source → transform → check → sink |
| `jpipe-cli` | PicoCLI entry point; fat JAR via Maven Shade |

Dependency order: `jpipe-lang` ← `jpipe-model` ← `jpipe-operators` ← `jpipe-compiler` ← `jpipe-cli`

All packages use `ca.mcscert.jpipe` as the base namespace.

---

## Build & Test

```bash
mvn package                              # build all modules
mvn test                                 # run all tests
mvn test -pl jpipe-model                 # single module
mvn test -pl jpipe-compiler              # E2E (Cucumber) tests
mvn test -pl jpipe-compiler -Dtest=Foo   # single test class
mvn verify                               # includes checkstyle and coverage
mvn spotless:apply                       # auto-format (required before commit)
mvn spotless:check                       # check formatting without applying
```

Code **must** pass `mvn spotless:apply` (Google Java Format) and `mvn verify`
before committing. CI will reject unformatted code.

---

## Codebase Exploration — MCP Tools

**This project has a knowledge graph (code-review-graph MCP server). Use it
before falling back to file search.**

| Goal | Use this first |
|------|----------------|
| Find a class or function | `semantic_search_nodes` |
| Understand blast radius of a change | `get_impact_radius` |
| Trace callers / callees / imports | `query_graph` with `callers_of` / `callees_of` |
| Review changed files | `detect_changes` + `get_review_context` |
| Architecture overview | `get_architecture_overview` + `list_communities` |

Fall back to `Grep` / `Glob` / `Read` only when the graph does not cover what
you need (e.g. content of newly created files not yet indexed).

---

## Compiler Pipeline Architecture

The compiler (`jpipe-compiler`) is built on a typed pipeline DSL
(ADR-0009, [`docs/design/compiler.md`](docs/design/compiler.md)):

```
FileSource → Transformation<I,M> → … → Checker<M> → … → Sink<O>
```

The four roles:
- **`Source<I>`** — reads the source file, produces the initial pipeline value
- **`Transformation<I,O>`** — converts one type to another; subclasses
  implement `protected run(I, ctx)`, callers use `final fire(I, ctx)` which
  handles logging, null detection, fatal fast-fail, and exception wrapping
- **`Checker<I>`** — `Transformation<I,I>`; guaranteed not to modify its input;
  used for validation steps
- **`Sink<O>`** — serialises the final value to a file or stream

`CompilerFactory` assembles two reusable sub-chains:

```java
// parsingChain(): InputStream → List<Command>
new FileSource()
    .andThen(new CharStreamProvider())
    .andThen(new Lexer())
    .andThen(new Parser())
    .andThen(new HaltAndCatchFire<>())
    .andThen(new ActionListProvider(operators, unification))
    .andThen(new LoadResolver(operators, unification));

// unitBuilder(): List<Command> → Unit
new ActionListInterpretation()
    .andThen(new ConsistencyChecker())
    .andThen(new CompletenessChecker());
```

`CompilationContext` is threaded through every step and carries the source path
and a `Diagnostic` bag. Severities: `ERROR` (non-fatal, accumulates) and
`FATAL`. `fire()` skips `run()` if the context already holds a fatal error.
`HaltAndCatchFire` is a checkpoint that promotes accumulated `ERROR`s to
`FATAL`. See [`docs/design/steps.md`](docs/design/steps.md) for the full step
reference.

---

## Domain Model Architecture

See [`docs/design/model.md`](docs/design/model.md). Key sealed hierarchies:

```
JustificationElement (sealed)
├── CommonElement — appears in Justifications and Templates
│   ├── Conclusion (required, one per model)
│   ├── SubConclusion
│   ├── Strategy
│   └── Evidence (record)
└── AbstractSupport (record) — template-only placeholder

SupportLeaf (marker interface) — implemented by SubConclusion, Evidence, AbstractSupport
```

`JustificationModel<E>` (sealed abstract base):
- `Justification` — accepts only `CommonElement`; concrete justification
- `Template` — accepts any `JustificationElement`; abstract/reusable structure

Model construction uses the **Command pattern** (ADR-0005). Traversal uses the
**Visitor pattern** (ADR-0008): `JustificationVisitor<R>`.

---

## Composition Operators

See [`docs/design/operators.md`](docs/design/operators.md) and ADR-0018.
Operators transform a list of source models into a command list via a sealed
four-phase algorithm (partition → create → link → additional). Built-in
operators: `refine`, `assemble`. Post-composition unification is handled by
`Unifier`.

---

## Design Decisions (ADRs)

Architecture decisions live in `docs/adr/`. Notable ones:

| ADR | Topic |
|-----|-------|
| ADR-0005 | Command pattern for model construction |
| ADR-0008 | Visitor pattern for model traversal |
| ADR-0009 | Typed pipeline abstraction |
| ADR-0011 | Cucumber for compiler end-to-end tests |
| ADR-0012 | Qualified ids and template expansion |
| ADR-0016 | Error management |
| ADR-0018 | Composition operators |

---

## Development Conventions

- **Diagnostics:** Use `CompilationContext` to report `ERROR` or `FATAL`
  diagnostics. Avoid throwing raw exceptions; prefer wrapping in
  `CompilationException` via the pipeline's `fire()` method.
- **Pipeline steps:** New compilation phases must be implemented as
  `Transformation<I,O>` or `Checker<I>` subclasses and placed under
  `ca.mcscert.jpipe.compiler.steps`.
- **Testing:** New features must include Cucumber scenarios in
  `jpipe-compiler/src/test/resources/features` and JUnit Jupiter unit tests in
  the relevant module.
- **Logging:** Log4j 2; follow ADR-0006 conventions.

---

## Naming Conventions

### Element IDs and qualified IDs

A plain id (`s`) is the form used in `.jd` source files. The compiler qualifies
it with the owning model or namespace at expansion time (`templateName:s`,
`ns:model:s`). `JustificationModel.findById` resolves both forms — callers
should pass the plain id when possible.

### Class naming

| Kind | Pattern | Examples |
|------|---------|---------|
| Model construction command | `Create<X>`, `Add<X>`, `Rewire<X>`, `Override<X>` | `CreateConclusion`, `AddSupport` |
| Compiler pipeline step | noun phrase for the step's role | `HaltAndCatchFire`, `SelectModel`, `LoadResolver` |
| Visitor / exporter | `<Format>Exporter`, `<Purpose>Visitor` | `DotExporter`, `PythonExporter`, `JsonExporter` |
| Validator | `<Concern>Validator` | `ConsistencyValidator`, `CompletenessValidator` |
| Test class | mirrors the class under test + `Test` suffix | `DotExporterTest`, `AddSupportTest` |

### Method naming

- Command execution entry point: `doExecute(Unit)` (called by `RegularCommand.execute`).
- Pipeline entry point: `run(I, CompilationContext)` (called by `Transformation.fire`).
- Visitor entry points: `visit(<Type>)` — one overload per node type.

---

## Code Quality Conventions

### Test code (AssertJ / JUnit 5)

- **Chain assertions**: use one `assertThat(x)` chain with multiple matchers — never
  split into separate `assertThat(x).contains(a)` + `assertThat(x).contains(b)` (S5853)
- **assertThatThrownBy**: the lambda must contain exactly the one throwing call — extract
  all setup outside the lambda (S5778)
- **hasToString**: prefer `assertThat(x).hasToString("…")` over
  `assertThat(x.toString()).isEqualTo("…")` (S5838)
- **Parameterized tests**: when 3+ test methods share identical structure with different
  inputs, use `@ParameterizedTest` + `@ValueSource` / `@CsvSource` (S5976)
- **No empty test bodies**: if a test intentionally does nothing, add
  `// intentionally empty` comment (S1186)
- **Unnamed catch variables**: use `_` instead of `e` / `ignored` when the variable is
  not read (S7467, Java 22+)

### Production code

- **No System.out/err**: use Log4j 2 (`private static final Logger logger =
  LogManager.getLogger(…)`) everywhere in production classes; add
  `@SuppressWarnings("java:S106")` only when stdout output is intentional by design (S106)
- **No raw `throws Exception`**: declare specific checked exception(s) or use the
  `RuntimeException` hierarchy (S112)
- **String constants**: if a literal appears 3+ times, extract
  `private static final String FOO = "…"` (S1192)
- **Lazy logger args**: pass expensive arguments as lambdas →
  `logger.debug("msg {}", () -> expensive)` (S2629)
- **StringBuilder.isEmpty()**: use `sb.isEmpty()` instead of `sb.length() > 0` (S7158,
  Java 15+)
- **Method references**: prefer `Foo::bar` over `x -> Foo.bar(x)` (S1612)
- **No unnecessary casts**: remove casts when the type is already compatible (S1905)
- **Cognitive complexity ≤ 15**: extract private helpers rather than nesting conditions;
  SonarQube flags methods that exceed this threshold (S3776)
- **No commented-out code**: delete dead code — git history preserves it (S125)
- **`@SuppressWarnings` requires explicit approval**: never add a suppression
  without proposing it to the user first and receiving a go-ahead. Always present
  the real alternative (refactoring, narrower type, etc.) before suggesting a
  suppression. Suppressions that are legitimately unavoidable (e.g. generated
  code, functional-interface `throws` boundaries) must still be approved and must
  carry an explanatory comment.
