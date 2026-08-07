# Changelog

All notable changes to **jPipe (Justified Pipelines)** are documented here.

This project reconstructs its history from git tags. Versioning was erratic in
the early days (skipped numbers, a lone `1.9.0`, a `v`-prefix switch at 2.0),
so dates are given for every release to make the timeline unambiguous. The
format loosely follows [Keep a Changelog](https://keepachangelog.com/).

> **A note on the history.** jPipe went through a full rewrite between the
> `0.2.x` line and `2.0.0`. The early versions (`0.0.1`–`0.2.8`) were a VS Code
> extension backed by a Langium language server and a first-generation Java
> compiler. Version `2.0.0` is a *tabula rasa*: a modular Maven / Java 25
> compiler with a typed pipeline, sealed domain model, and composition
> operators. `1.9.0` is a transitional tag captured during that migration.

---

## [Unreleased]

### Added
- jPipe can now be installed on Windows with [Scoop](https://scoop.sh):
  `scoop bucket add mcscert https://github.com/jpipe-mcscert/scoop-mcscert`
  followed by `scoop install jpipe`. Releases publish an additional
  `jpipe-<version>.zip` asset, and the release pipeline keeps the bucket
  manifest up to date automatically (ADR-0025).

---

## [2.2.0] — 2026-07-18

### Added
- A model implementing a template may now only override the template's
  `@support` placeholders; referencing a template-internal element (a strategy
  or conclusion) is rejected with a `[reference-into-template]` error.

### Changed
- PPA packages are now published for Ubuntu `noble` (24.04 LTS), `resolute`
  (26.04 LTS), and `stonking` per the new Ubuntu release target policy
  (ADR-0023); `jammy` (22.04 LTS) and `questing` (25.10) are no longer targeted.

### Removed
- The rolling `unstable` GitHub pre-release and its force-pushed `unstable` tag
  are retired (ADR-0024); use the tip of `dev` or a per-run CI build artifact for
  the latest integrated build.

### Fixed
- DOT export now quotes template cluster ids, so a namespaced template name
  (e.g. loaded via `load "…" as ns`) no longer produces a malformed Graphviz
  graph.
- Overriding a template's `@support` with an evidence no longer leaves a stale
  placeholder reference on the inherited strategy, which could render a
  duplicate arrow in DOT export.

---

## [2.1.0] — 2026-07-15

### Added
- `load` directive now supports **glob patterns** for loading multiple files
  at once (#136).
- JSON and Python exports **preserve unification aliases** produced by
  composition operators (#135).

### Changed
- Updated build and test dependencies: `org.json` (→ 20260522), JUnit
  (→ 5.14.2 / Platform 1.14.2), Cucumber (→ 7.34.4), Checkstyle (→ 13.8.0),
  and the JaCoCo, Shade, Spotless, Sonar, and surefire tree-reporter plugins.
  JUnit 6 was deferred — Cucumber 7.34.4 targets JUnit Platform 1.14 and is not
  yet compatible with Platform 6.

### Fixed
- Malformed globs and I/O failures in the `load` directive are now reported as
  proper diagnostics instead of crashing.

---

## [2.0.4] — 2026-05-20

### Added
- End-to-end scenarios covering invalid `load` cases (circular loads and flat
  namespace collisions).

### Fixed
- Repeated loads of the same file are now **idempotent** (#134).
- Duplicate-load warnings print the flattened id instead of `null`.
- Debian packaging: launcher pinned to the declared Java 25 dependency;
  `postinst` and ADR-0021 review fixes.

## [2.0.3] — 2026-05-07

### Fixed
- **Composition operators:** transitive aliases now propagate correctly across
  chained composition steps.
- **`refine` operator:** hook ids are normalized through `findById` (suffix
  fallback), hook elements resolve through the unit alias map after
  composition, and the operator fails early when a hook element is missing from
  the base model.

### Changed
- Improved unit-test coverage for operators and the domain model.

## [2.0.2] — 2026-04-21

### Fixed
- **Python export:** activated the `@jpipe_link` decorator and corrected its
  import path.

## [2.0.1] — 2026-04-21

### Fixed
- **Packaging:** renamed `homebrew/` to `bin/`, fixing the launcher and Debian
  packaging layout.

## [2.0.0] — 2026-04-20

The rewrite. jPipe is now a multi-module Maven project on Java 25, replacing
the previous VS Code extension / Langium stack.

### Added
- **Modular architecture** — `jpipe-lang`, `jpipe-model`, `jpipe-operators`,
  `jpipe-compiler`, `jpipe-cli`.
- **Typed compilation pipeline** (ADR-0009): `Source → Transformation → Checker
  → Sink`, threaded through a `CompilationContext` diagnostic bag.
- **Sealed domain model** built with the Command pattern (ADR-0005) and
  traversed with the Visitor pattern (ADR-0008).
- **Composition operators** framework (ADR-0018) with built-in `refine` and
  `assemble`, plus automatic post-composition **unification**.
- **Templates** (formerly "patterns") with qualified ids and template inlining
  (ADR-0012), abstract-support overrides, and multi-level inheritance.
- **`load` directive** implemented as a compiler pipeline step.
- **Exporters:** Graphviz DOT/PNG/JPEG, SVG, JSON, and Python — with model
  selection, qualified node ids, accessibility-aware styling, and robust label
  escaping/wrapping.
- **Consistency & completeness validation:** single-conclusion enforcement,
  acyclic-implements rule, unresolved-override detection, unknown-symbol
  diagnostics.
- **Structured error management** (ADR-0016) with source-location traceability
  and a diagnostic report.
- **Logging conventions** (ADR-0006) and a `--log-level` CLI option.
- **CLI** restructured into subcommands (`process` as default, `diagnostic`,
  `doctor`), with a `-m/--model` selector and version reporting.
- **Packaging & release:** Debian package, versioned Homebrew formula, rolling
  `unstable` release, and a tag-triggered release pipeline.
- Extensive **design docs**, ADRs, railroad diagrams, and a usage guide;
  Javadoc integrated into the MkDocs site.

### Changed
- Namespace settled on `ca.mcscert.jpipe`.
- Sources reformatted to an 80-column limit (Google Java Format).
- Large SonarCloud-driven quality pass: cognitive-complexity reductions,
  string-constant extraction, parameterized tests, dead-code removal, and
  80%+ coverage gates.

### Notes
- `v2.0.0-rc1` (2026-04-20) was the release candidate that shook out the CI and
  release-workflow issues ahead of the final tag.

---

## [1.9.0] — 2025-12-09

Transitional tag captured mid-rewrite (main branch was reset — "wiping out main
branch").

### Added
- **Symbol tables:** `SymbolTree` / `SymbolNode`, and a `RepTable` supporting
  multiple parents per element.
- **Composition operators:** working `assemble` and `refine`, plus a `merge`
  operator.
- Renamed **patterns → templates**; pattern realization via hierarchic symbol
  tables.
- **JSON export**, and SVG export that preserves element ids.
- GraalVM-ready build profile; SonarQube analysis wired in.

### Changed
- Redesigned cloning (dropped `Cloneable`); `load` reworked as a macro action.

---

## [0.2.8] — 2025-03-06
### Fixed
- Double-quote handling in the release workflow.

## [0.2.7] — 2025-03-01
### Added
- Configurable **Java version** for image generation (#98).
- Completion for variables found in **loaded files** (#96); instruction text on
  completion prompts (#82).
- Filesystem wrapper class to insulate against `fs` API changes.
### Fixed
- Path normalization for input files.

## [0.2.6] — 2025-01-29
### Fixed
- `uri`-to-`fsPath` error in image generation.

## [0.2.5] — 2025-01-29
### Fixed
- Command-line generation.

## [0.2.4] — 2025-01-20

(Version `0.2.3` was never tagged.)

### Added
- **Output-channel manager** supporting multiple output channels.
- **Installation checks** for Graphviz and Java on startup (toggleable).
- **`load` statement** support, with links resolving into referenced files.
- Conclusion validation and quick-fixes (#74); relative-path recognition
  without `./`.
### Changed
- Consolidated developer-mode and JAR-file configuration into a configuration
  manager.
### Fixed
- Startup issue with the default JAR-file setting.
- Outdated dependencies.

## [0.2.2] — 2024-08-06
### Fixed
- Missing JAR in the published extension.

## [0.2.1] — 2024-08-06

### Added
- **Action-based compiler** finalized as the regular compiler; transformation
  chain expressed as functional composition.
- **Compositions and patterns** introduced in the model.
- **Scope provider** (strict, loaded-files-only) and validation of
  justification-diagram variable types.
- Code action to remove an `@support` line; diagnostic codes on diagnostics.
- First ADR added; checkstyle compliance and warning-suppression support.
### Changed
- Complete refactor of the validation service for easier future checks.
- Reworked justification and pattern grammar.

## [0.2.0] — 2024-07-20

### Added
- **Release / CD infrastructure:** modular CI/CD pipelines, manual-dispatch
  triggers, and **Homebrew** distribution support.
### Changed
- Restructured the release workflow so extension and compiler build
  independently.

---

## [0.1.0] — 2024-07-18

The first substantial release: a VS Code extension with a Langium language
server.

### Added
- **Langium grammar** for jPipe with cross-references, hovering, and
  context-scoped completion for supporting statements.
- **Language server (LSP)** integration with the extension client.
- **Live preview panel:** updates on cursor position, tab change, and
  selection; SVG rendering; reopen-after-close support.
- **Image export:** save diagrams as PNG/PDF via context-menu commands.
- **Command registration system** and an observer-pattern update manager.
- **CLI groundwork:** `-h/--help`, input/output/diagram options, exception-based
  error handling, `--all` to flush every diagram.
- **Diagram merging** (MVP) with a merge-process tracker.
- Skeleton of the **action-based compiler**; errors as a singleton log with
  text/JSON export; a color printer.
- Initial GitHub Actions (grammar generation, `.vsix` build artifact).
### Changed
- Configurable JAR-file path and log-level preferences; client-side config
  updates.
- Large checkstyle-compliance and refactoring pass across the model, builders,
  and exporters.

## [0.0.1] — 2023-07-03
### Added
- Initial commit: first version of the compiler.

[Unreleased]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.2.0...HEAD
[2.2.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.1.0...v2.2.0
[2.1.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.0.4...v2.1.0
[2.0.4]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.0.3...v2.0.4
[2.0.3]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.0.2...v2.0.3
[2.0.2]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.0.1...v2.0.2
[2.0.1]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.0.0...v2.0.1
[2.0.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/1.9.0...v2.0.0
[1.9.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/0.2.8...1.9.0
[0.2.8]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/0.2.7...0.2.8
[0.2.7]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/0.2.6...0.2.7
[0.2.6]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/0.2.5...0.2.6
[0.2.5]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/0.2.4...0.2.5
[0.2.4]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/0.2.2...0.2.4
[0.2.2]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/0.2.1...0.2.2
[0.2.1]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/0.2.0...0.2.1
[0.2.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/0.1.0...0.2.0
[0.1.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/0.0.1...0.1.0
[0.0.1]: https://github.com/jpipe-mcscert/jpipe-compiler/releases/tag/0.0.1
