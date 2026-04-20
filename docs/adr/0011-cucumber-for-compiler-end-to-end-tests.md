# ADR-0011: Cucumber for Compiler End-to-End Tests

**Date:** 2026-03-27
**Status:** Accepted

## Context

The compiler pipeline (ADR-0009) transforms `.jd` source files into internal
models and output artefacts through several typed steps. Unit tests cover
individual steps and pipeline mechanics in isolation. However, end-to-end
behaviour — does a given `.jd` file compile into the expected `Unit`? — is not
captured by unit tests, which use synthetic in-memory inputs.

Two approaches were considered for end-to-end testing:

- **JUnit 5 parameterised tests over fixture files** — test cases are `.jd`
  input files paired with programmatic assertions. Concise and dependency-free,
  but the test intent is expressed in Java code, making scenarios hard to read
  without understanding the model API.
- **Cucumber (BDD) with Gherkin feature files** — scenarios are written in
  structured natural language (`Given / When / Then`). Step definitions
  translate each step into pipeline invocations and AssertJ assertions. The
  `cucumber-java` and `cucumber-junit-platform-engine` dependencies were already
  present in the root POM.

## Decision

All compiler end-to-end tests are written as Cucumber scenarios.

- Feature files live in `jpipe-compiler/src/test/resources/features/`.
- `.jd` fixture files live in the top-level `examples/` directory, making them
  discoverable as language reference material independently of the test suite.
- Step definitions live in the `ca.mcscert.jpipe.compiler.e2e` package.
- The `@When` step drives the `parsingChain().andThen(unitBuilder())` pipeline
  directly as a `Transformation<InputStream, Unit>`, with no `Sink` and no file
  I/O, keeping scenarios in-memory.
- The suite is registered via a `@Suite`-annotated `CucumberTestSuite` class so
  that Maven Surefire discovers it automatically. Scenarios run as part of
  `mvn verify` without any extra flags.
- The `examples.dir` system property is injected by Surefire
  (`${project.basedir}/../examples`) so that step definitions can resolve
  fixture files by name without hard-coded paths.

## Rationale

- Gherkin scenarios read as specifications: a new contributor can understand
  what a compiled `.jd` file is expected to produce without reading Java code.
- Decoupling fixture files from test code (top-level `examples/` vs.
  `src/test/resources/`) lets the same files serve as both test inputs and
  language documentation.
- Driving the pipeline as a plain `Transformation` (rather than through
  `Compiler.compile`) removes the need for temporary files in tests and makes
  the `When` step a direct exercise of the public pipeline API.
- Cucumber was already a declared dependency; no new third-party library is
  introduced.

## Feature file formatting

`And` and `But` steps are indented two extra spaces relative to the enclosing
`Given`, `When`, or `Then` step, visually grouping continuations under their
parent:

```gherkin
    When examining justification "simple"
    Then it has a conclusion with id "c" and label "The system is correct"
      And it has a strategy with id "s" and label "Testing"
      And the strategy "s" supports the conclusion "c"
```

Spotless is configured for Java only and does not touch `.feature` files, so
this indentation is preserved as written. New feature files must follow this
convention manually.

## Consequences

- Every new end-to-end compiler behaviour must be captured as a Cucumber
  scenario in a `.feature` file before or alongside the implementation
  (specification by example).
- Fixture `.jd` files added to `examples/` must be valid jPipe source that
  compiles without errors, unless they are explicitly intended to test error
  cases (in which case the scenario should assert on diagnostics, not on the
  unit).
- Step definitions in `ca.mcscert.jpipe.compiler.e2e` are the only place
  allowed to call `CompilerFactory` from test code. Scenarios that need
  assertions beyond the existing steps must extend `CompilationSteps` with new
  `@Then` methods rather than adding ad-hoc JUnit tests.
- Unit tests (JUnit 5 + AssertJ) remain the right tool for individual pipeline
  steps and model classes; Cucumber is reserved for scenarios that exercise the
  full `parsingChain + unitBuilder` path with a real `.jd` file.
