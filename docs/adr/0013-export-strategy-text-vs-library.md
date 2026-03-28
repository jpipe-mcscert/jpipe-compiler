# ADR-0013: Export Strategy — Text Generation vs. Library-Backed IR

**Date:** 2026-03-28
**Status:** Accepted

## Context

The compiler supports multiple output formats (`.jd`, DOT, JSON, Python, …). For each format
there are two implementation strategies:

- **Text generation** — a `JustificationVisitor` walks the model and writes the target syntax
  directly to a `StringBuilder`. No library dependency; the format is produced as a string.
- **Library-backed IR** — the visitor populates a library-specific intermediate representation
  (e.g. `guru.nidi.graphviz.model.MutableGraph` for Graphviz), which the library then
  serialises or renders.

The original codebase (main branch) used `guru.nidi.graphviz` for DOT/PNG/SVG export.
The library builds an in-memory graph object that can be rendered to multiple image formats
in-process. This is convenient when rendering is the goal, but it couples a heavy dependency
(~20 MB, includes Batik and a JS engine for layout) to what is, for DOT output, a trivially
serialisable text format.

jPipe is intended to stay lean. Unnecessary dependencies increase JAR size, complicate the
Shade assembly (see ADR-0003), and add transitive risk without proportional benefit.

## Decision

**Text-based formats are produced by direct text generation — no library-backed IR.**

A `JustificationVisitor` implementation writes the target syntax directly. This applies to any
format whose output is a text file: `.jd`, DOT (`.dot`/`.gv`), JSON, Python, and any future
text format.

**Formats requiring rendering (PNG, SVG, …) delegate to an external tool via process invocation.**

When the output cannot be produced as text by the JVM alone, jPipe shells out to the appropriate
command-line tool (e.g. `dot` for Graphviz rendering). The tool is a declared system dependency,
documented in the project README alongside its minimum version and installation instructions.

A `--doctor` CLI option will be added when the first rendering format is implemented. Its sole
responsibility is to verify that declared external tools are available on `PATH` and report
clearly if they are not. Input-argument consistency (e.g. `--format png` requires `--output
<file>`) is handled separately.

## Rationale

- DOT syntax, like `.jd` syntax, is a well-defined text grammar. Producing it via a library
  adds indirection without adding correctness — the library would simply serialise the same
  structure the visitor already holds.
- The rendering capability of `guru.nidi.graphviz` (in-process PNG/SVG via Batik and V8) is the
  only part that cannot be replicated with a StringBuilder. Shelling out to `dot` achieves the
  same rendering quality, since `dot` is the canonical Graphviz renderer. The only trade-off is
  a runtime rather than compile-time dependency — mitigated by `--doctor`.
- Text generation is straightforward to test: string assertions on the output are simpler and
  more readable than assertions on a library-specific object graph.
- Keeping the library off the classpath removes a class of Shade transformer problems (see
  ADR-0003) before they arise.

## Consequences

- New text-format exporters are implemented as `JustificationVisitor` in `jpipe-model`
  (ADR-0010), with no format-library dependency.
- PNG and SVG export, when implemented, will invoke `dot` as a subprocess. `CompilerFactory`
  will build a pipeline that ends with a process-invocation sink rather than a `StringSink`.
- External tool requirements are documented in the README. `--doctor` will check PATH
  availability for each declared tool; it will be introduced alongside the first rendering
  format.
- `guru.nidi.graphviz` is not added to any module's `pom.xml`. If a future need arises that
  cannot be satisfied by process invocation (e.g. embedding jPipe in an environment without
  a shell), the decision should be revisited with a new ADR.
