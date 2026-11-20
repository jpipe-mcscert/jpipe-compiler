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

---

## [4.0.0] — 2026-11-19

### Added
- Strings accept two escapes: `\"` for a double quote and `\\` for a backslash.
  Any other backslash is kept as written, so a path such as `"dir\file.jd"`
  needs no change. The JPIPE export escapes labels the same way, so a label
  holding a quote compiles back to itself (#176).
- The JSON export gives each element of a model built by an operator its
  `origins`: the elements of the models it was composed from, at every level,
  each with its model, its id and the kind it had there. A consumer such as
  jpipe-runner can now tell which elements composition changed, and what they
  were: the hook `refine` merges into a sub-conclusion, or the conclusion of a
  source that `assemble` turns into a sub-conclusion. A model that implements
  a template built by an operator passes the template's origins on to the
  elements it inherits. The new key is optional, and the JSON of a model that
  no operator built, and that implements no template an operator built, is
  unchanged (#200).
- The JSON export of a model that implements a template describes how the DOT
  export draws the template chain, in a new `implements` key: each template's
  cluster and the elements drawn in it, the `@support` placeholders and their
  labels, which element fills each one, and which edges are drawn from a
  placeholder. A consumer such as jpipe-runner can now draw the model as the
  compiler does. `elements` and `relations` are unchanged, and the JSON of a
  model that implements nothing is unchanged. The DOT export now writes its
  override arrows in drawing order, so their order can change once in a DOT
  file generated with an earlier version (#199).

### Changed
- **Breaking:** mistakes in an operator call are errors at the call instead of
  being accepted (#184):
  - `implements` on a model built by an operator, which was dropped;
  - a configuration key given twice, where the last value won;
  - a key the operator does not accept, such as a misspelt `unifyExlude` or a
    `hook` passed to `assemble`, which was ignored;
  - an `unifyExclude` id that names no element of the result, such as a plain
    `e` where the result names it `a:e`, which was ignored.
- **Breaking:** `refine`'s hook must be an evidence or a `@support`. A strategy
  or a conclusion was accepted, and broke the model (#181).
- **Breaking:** a source model given more than once to an operator
  (`assemble(a, a)`) is an error (#181).
- `refine` produces a template when its result keeps a `@support`, so a template
  can be refined; it could only produce a justification (#181).
- **Breaking, for custom operators:** `CompositionOperator.resultKind` is
  removed. The kind of the result is the model `createResultModel` creates,
  which now also receives the commands creating the result's elements, and an
  operator declares the optional keys it reads with `optionalArguments()`
  (#181, #184).
- A mistake in an operator call is reported with the code
  `invalid-operator-call`, at the call: an unknown operator, a missing, unknown
  or repeated argument, a hook that cannot be refined, a source given twice.
  These were `execution-error` (#184).
- The documentation now states that unification merges whatever its equivalence
  relation deems equivalent: under the default `sameLabel`, two strategies with
  the same label are merged and pool their evidence, and an element the operator
  creates is merged like any other (#188, #181).
- **Breaking:** a model that implements a template inherits its conclusion and
  can no longer declare its own: that is now an `inherited-conclusion` error. It
  used to be accepted or rejected depending on whether the template was declared
  before or after the model (#177).
- **Breaking:** a relation or an override whose id is the end of several
  inherited ids, such as `a` when both `root:a` and `mid:a` are inherited, or
  `t:a` for both `x:t:a` and `y:t:a`, is now an `ambiguous-id` error asking for
  the full qualified id. It used to bind to the first one found (#177).
- **Breaking:** a template's `@support` that supports no strategy is now an error
  in that template (completeness rule `abstract-support-used`). It compiled, but
  no justification could override it, and the failure was only reported, without
  a line, in each one that tried (#183).
- A model declared under a name already taken, in the same file or in a loaded
  one, is reported once, at the second declaration and naming where the first
  is, with the new code `duplicate-model`. It used to be an `execution-error`,
  plus a misleading `single-conclusion` error. Nothing in the second
  declaration is added to the first model any more (#183).
- **Breaking:** a string now ends at its first unescaped double quote. It used
  to run to the last quote on its line, so a trailing `// see "doc"` comment
  became part of the label, and two declarations written on one line were
  merged into one element. A label that holds a double quote must now write it
  `\"` (#176).
- **Breaking:** two backslashes in a string now stand for one. A label written
  `"a\\b"` reads `a\b`, and a Windows network path in a `load` must double
  its leading backslashes, `"\\\\server\\share\\lib.jd"`, or use forward
  slashes (#176).
- **Breaking:** a character the language does not know, such as the `-` in
  `e-1` or the `é` in `café`, is now a syntax error reported where it sits. It
  used to be dropped without a word, so `e-1` and `e-2` both became `e` (#176).
- The compilation errors of `jpipe process` are now printed on standard error as
  bare `file:line:column: error: message` lines, without a timestamp or logger
  name, and no `--log-level` hides them: `--log-level OFF` used to leave only
  `Compilation failed`, with no line or column (#186).
- **Breaking:** `--log-level` now rejects an unknown level with exit code 2, as
  `-f` does. It used to fall back to `ERROR` without a word (#186).
- The `jpipe` launchers installed by Homebrew, Scoop and the Debian package no
  longer refuse to start when Graphviz is missing. That blocked `jpipe doctor`,
  which exists to diagnose it, and every format that does not need it; the image
  formats still report a missing `dot` (#186).
- **Breaking:** a source file that is not valid UTF-8 is now a fatal error,
  located at the first invalid byte. Its bytes used to be replaced with `�`
  without a word, so a file saved in Latin-1 compiled with mangled labels
  (#179). A UTF-8 file that starts with a byte-order mark is still accepted.
- Updated build and test dependencies to their current majors: JUnit 6
  (→ 6.1.3, Jupiter and Platform), Cucumber 8 (→ 8.0.4), Checkstyle 14
  (→ 14.4.0) and `json-schema-validator` 3 (→ 3.0.9). This lifts the JUnit 6
  deferral noted in 2.1.0: Cucumber 8 targets JUnit Platform 6. Every CI
  workflow now runs the latest major of each GitHub Action.
- Python export now imports `Path` from `pathlib` in every module that holds an
  evidence, since jpipe-runner injects each artifact an evidence `observes` as a
  `Path` parameter. Annotating those parameters no longer needs an import added
  by hand.
- Python export now hard-wraps the boilerplate of the generated module at 80
  columns, commented-out steps included: `jpipe_runner` is imported on two
  lines, one for the decorators and one for the outcomes; a long decorator,
  docstring or `NotImplementedError` message is broken over several lines; and
  the header and the note above a commented-out step each take two. What comes
  from the model has nowhere to break and can still run past the limit: a
  function name, which follows its label, an id, or the source path in the
  header.
- **Breaking:** a Python function name keeps the letters of a label in a
  non-Latin script, which Python 3 accepts in identifiers: `"系统 正确"` gives
  `def 系统_正确()` instead of `def _()`. A letter with a diacritic still maps to
  its ASCII base (`"Vérification"` still gives `verification`), but a letter
  that has none is now kept instead of dropped (`ß`, `ø`, `œ`): a step library
  generated for such labels must be regenerated or renamed (#187).
- **Breaking:** the JSON export no longer writes an `escaped` field on each
  element. It was undocumented, and it was not the Python function name whenever
  two names collided or a label was a reserved word (#187).
- **Breaking:** in the DOT export, and the SVG drawn from it, an overridden
  `@support`'s placeholder node now has its own `id`, ending in `@abstract`. It
  shared the id of the element overriding it, so a lookup by id, such as the
  VS Code preview's, could land on either one (#187).
- The header of a Python export no longer carries the generation date, so
  regenerating a committed step library leaves it unchanged (#187).

### Fixed
- On Windows, compiling a source read from standard input no longer fails with
  an internal error (exit code 42).
- An id repeated in `unifyExclude` (`"c,c"`) no longer fails the composition with
  a misleading `duplicate element` error: the list is a set (#171).
- A plain `refine` hook that names several elements of the base model is an
  `ambiguous hook` error, instead of silently taking the first (#181).
- A `refine` hook is found through every composition step that renamed it, not
  only the last one (#181).
- The documentation, the language reference included, gives the `refine` hook as
  an element id (`first:e`); it gave a `base/elementId` form that never worked
  (#181).
- A model declared as a justification but built by `refine` as a template, or
  the other way round, is reported only once the call is otherwise valid: a
  mistake in the hook is reported as itself, not as a kind mismatch (#181).
- A file that compiles on its own now also compiles once loaded under a
  namespace (`load "f.jd" as ns`) (#178):
  - its operator calls are prefixed like its models, the element ids of their
    `hook` and `unifyExclude` included, however many composition steps they go
    through; the calls used to wait forever for their unprefixed sources;
  - its overrides and relations that use a qualified id, such as
    `evidence base:t:abs`, are prefixed too; the overrides used to be lost;
  - a file that two namespaced files both load is loaded in each namespace; the
    second one used to be skipped, leaving unknown models;
  - a file loaded flat inside a namespaced file is loaded into that namespace,
    even when it is also loaded flat elsewhere.
- The same file reached through a symbolic link, or spelled differently on a
  case-insensitive file system, is loaded once instead of twice (#178).
- A `load` path the file system does not accept is a located error instead of a
  Java stack trace, and a glob that meets a directory it cannot read names that
  directory instead of a Java exception (#178).
- The order of declarations no longer changes what a model means (#177):
  - a template is copied whole into the model that implements it, even when its
    relations are written after that model;
  - a chain of templates declared bottom-up (`j implements mid`, then
    `mid implements root`, then `root`) is inherited in full;
  - a short id in a relation binds to the model's own element, even when that
    element is declared after the relation, instead of to an inherited one;
  - a relation into an abstract support may be written before its override;
  - an operator call composes its source models once they are complete;
  - a model lists its elements in the same order whether its template is
    declared before or after it, so a composition of that model gives the
    elements it unifies the same ids (`unified_0`, `unified_1`, …).
- A relation naming the inherited conclusion by its short id (`s2 supports c`)
  is reported as `reference-into-template`, as for any element of the template
  other than a `@support`, instead of as an unknown element (#177).
- A mistake in one model no longer holds back what does not depend on it: a
  model with a broken relation is still composed by the operator calls that use
  it, and a stuck override no longer blocks the other relations of its model
  (#177).
- A model whose `implements` fails or never completes, such as two templates
  implementing each other, is reported once. Its other statements and its
  completeness are no longer reported as mistakes of their own, and no operator
  call consumes it (#177). A cycle that goes through an operator call, such as
  `j implements t` where `t` is `assemble(j, a)`, is reported as
  `cyclic-implements` too.
- An error in a `load`-ed file is reported in that file, at its own line. It was
  reported against the file being compiled, often at a line that file does not
  have, so the IDE pointed at the wrong place (#173).
- One failing statement no longer stops the construction of its model: the
  statements after it still run, so they are no longer reported as missing, as
  in `Strategy 's2' has no supporting element` after a conclusion was given two
  strategies. What only waits on a model whose operator call failed, however
  indirectly, adds no error of its own (#183).
- Diagnostics carry the line of the statement that caused them (#183):
  - a failing operator call is reported at the call, names the operator
    (`'assemble' for 'r'`, not `AssembleOperator`), and no longer repeats
    `[execution-error]` in its message;
  - a call to a model that does not exist names that model;
  - a `load` that fails is reported at its `load` statement, naming the path as
    written;
  - duplicate models, a second conclusion and other failing statements give
    their line.
- Overriding an abstract support twice, or overriding an element that is not
  one, is reported as `unresolved-override` at the override, instead of only
  `unresolved symbol(s) — model cannot be built` (#183).
- `jpipe diagnostic` no longer reports `"status": "ok"` and exits 0 on a source
  it never read, such as standard input redirected from a directory: the
  failure is a fatal diagnostic, with exit code 1. Under `process`, that source
  is a system error (exit code 42), as one that cannot be opened is (#180).
- A single-quoted label (`'A label'`) is now a syntax error at its location,
  instead of a crash with a Java stack trace and no diagnostic report. Strings
  are written between double quotes only (#172).
- The logo banner is now printed on standard error. It used to start every
  export written to standard output, so `jpipe process` without `--headless`
  produced JSON, Python and images that could not be read (#175).
- An unexpected failure now prints one `internal error: …` line and exits with
  code 42, as documented, instead of a Java stack trace and exit code 1, which
  looked like an ordinary compile error. `--log-level DEBUG` shows the trace
  (#182).
- `jpipe process -f PYTHON` on a model in which two elements share an id now
  fails like any other compile error, with exit code 1 and no stack trace
  (#182).
- `jpipe --log-level=INFO …`, the form `--help` shows, is accepted when no
  subcommand is given; it used to fail as an unknown option (#186).
- An argument that happens to be a subcommand name, such as a model called
  `doctor`, no longer stops `process` from being the default subcommand (#186).
- `-V` on a subcommand (`jpipe process -V`) prints the version; it printed
  nothing (#186).
- The error for a missing Graphviz now suggests `jpipe doctor`; it suggested
  `jpipe --doctor`, which does not exist (#186).
- `-o` no longer truncates its file before compiling. An `-o` naming the input
  file erased the source; it is now refused with exit code 2. A run that
  aborts, on a fatal error or an unknown model, wiped the previous output; the
  file is now written only once the command has produced its output (#174).
- An `-o` that cannot be written (a directory, a read-only file, a missing or
  read-only directory) is reported as `error: cannot write '<path>': <reason>` before
  compiling, instead of `unexpected error: …` (#186).
- Text exports and reports written to standard output are now always UTF-8.
  They followed the terminal's encoding, so on a non-UTF-8 console, the Windows
  console included, every non-ASCII letter became `?` (#179).
- A failure to write an export or a report, such as a full disk or a closed
  pipe, is now an error with exit code 42. Text formats used to exit 0 with an
  empty or missing file (#179).
- The DOT export of a justification no longer draws edges from a template's
  evidence into a strategy the justification declares itself, without support,
  under the same short id as a template strategy. Such a strategy is now drawn
  with no incoming edge, as declared (#189).
- Python function names, and the severities of the diagnostic report, are now
  lowercased the same way under every system locale. Under a Turkish locale,
  `"CHECK INPUTS"` gave `check_nputs` (#187).
- The JPIPE export compiles back to the model exported, as documented. It
  did not for a model that implements a template, which declared again every
  element it inherits; for a composed model, whose qualified ids no body can
  declare; nor for a model loaded under a namespace, whose name the grammar
  rejects. The output now holds, first, every model the exported one depends
  on; a model that implements a template declares only its own elements,
  overrides and relations; a composed model is written as its operator call;
  and a model loaded under a namespace is inlined under a plain name (`base:t`
  becomes `base_t`, and `base:t:abs` becomes `base_t:abs`), never as a
  `load` (#185).

---

## [3.0.0] — 2026-11-10

### Changed
- **Breaking:** `jpipe process -f PYTHON` now writes step libraries for
  [jpipe-runner 4.0 or later](https://github.com/jpipe-mcscert/jpipe-runner/blob/main/docs/authoring.md),
  which replaced the v3 authoring API the export targeted until now (#165).
  Each element becomes one function under the decorator of its kind
  (`@evidence`, `@strategy`, `@sub_conclusion`, `@conclusion`), taking the
  element's ids as positional arguments and empty `observes`/`consumes`/`produces`
  declarations to fill in. It returns an `Outcome` and raises
  `NotImplementedError` until it is written. The module imports from
  `jpipe_runner` and its header names the runner it targets. A dry run of a fresh
  module (`jpipe-runner --dry-run`) lists what is left to write: every evidence
  that observes nothing (`JP018`) or produces nothing (`JP012`).
- Python export now emits the conclusion again, commented out like
  sub-conclusions, since jpipe-runner 4 runs a bound conclusion after everything
  that supports it. Uncommenting it works as is: the module already imports its
  decorator.

### Fixed
- Python export no longer loses a step when two elements share a label. Both
  became functions of the same name, so the second replaced the first and the
  runner never saw the step it held, which is common in composed models. Such
  functions are now prefixed with their namespace (`a_claim_a_strategy`), or with
  the id they are bound by when they share one (`expected_minimal_s_a_strategy`).
  A label that escapes to a Python keyword (`Pass`) or to the name of a runner
  decorator (`Strategy`, `Evidence`) is prefixed the same way, rather than
  yielding a module that does not parse or that replaces the decorator.

**Upgrading:** step libraries generated by jPipe 2.5.0 or earlier target
jpipe-runner below 4.0 and stop at their first import on 4.0 or later. Either keep
running them with `jpipe-runner<4.0`, or port them by hand following the runner's
[migration guide](https://github.com/jpipe-mcscert/jpipe-runner/blob/main/docs/migration-v3-v4.md).

---

## [2.5.0] — 2026-08-14

### Added
- Python export now leaves out the conclusion entirely and emits sub-conclusions
  commented out, with a line saying why. The runner requires only evidence and
  strategies to be bound: it never executes a conclusion, so there was nothing to
  implement there, and it passes an unbound sub-conclusion through. Commenting
  the sub-conclusions keeps the shape of the argument visible and lets you turn
  one into a real check by deleting the `# ` prefixes. Both remain in the JSON
  and DOT exports, which describe the model rather than its execution.
- Python export now groups functions by namespace, each section opening with a
  banner comment naming it, so a module assembled from several sources reads as
  the parts it was built from rather than one flat list. Sections are ordered
  alphabetically; elements a composition created or merged carry no source
  prefix and are grouped under the composed model itself. Within a section
  elements run bottom-up (evidence, then strategies, then sub-conclusions), so
  the file reads from the steps you implement toward the claim they support.

  ```python
  ###       ###
  ## a_claim ##
  ###       ###
  ```
- Python export now guarantees that every `@jpipe_link` in a generated module
  names a distinct identifier, and refuses to write the module otherwise
  rather than emitting one that cannot load. A runner keys its binding registry
  by the link id, so the same id on two functions leaves it with two candidate
  implementations and no way to choose.
- New `unique-identifiers` consistency rule: compilation now fails if any
  identifier an exported model can be referenced by would designate two
  different elements. Element ids were already checked; this extends the
  guarantee to the aliases a merge leaves behind, because an alias colliding
  with another element's id makes a reference ambiguous. Consumers index ids
  and aliases into one namespace and cannot resolve such a clash —
  `jpipe-runner` discards the entire model rather than guess — so the compiler
  now reports it as an error, with the source location, instead of emitting a
  model that silently fails to load.

### Changed
- `jpipe doctor` now reports the version of each external tool it finds
  (e.g. `dot (Graphviz): OK (version 12.2.1)`). Some operating systems ship an
  outdated Graphviz, which is a common explanation for rendering problems.

### Fixed
- **Python export:** `@jpipe_link` now names elements the way you wrote them.
  An element produced by a merge is linked by the ids it was merged from
  instead of by the internal id unification minted for it (`unified_0`), and a
  composition of a composition resolves all the way back to the ids in the
  original source models. Links are also shortened — `@jpipe_link("a_claim:s1")`
  rather than `@jpipe_link("assembled_2:a_claim:s1")` — but never below
  `justification:id`, since a bare `s1` would leave a reader of the generated
  module unable to tell what it refers to, and they lengthen again wherever a
  shorter form would be ambiguous. Elements a composition operator creates in
  its own right, such as `assembleConclusion`, keep their id: it is the only
  name they have, and their label comes from the operator call.
- **JSON export:** the per-element `aliases` array now lists every pre-merge id
  rather than only the most recent one. Composing the result of a composition
  records a chain of merges, and the earlier ids in that chain were dropped,
  leaving elements that `jpipe-runner` could not bind by their original names.
- `jpipe diagnostic` now reports an unreadable input file as a fatal diagnostic
  and exits 1, instead of exiting 42 with only a message on standard error.
  `jpipe process` is unchanged: a missing file there is still a system error.
- `jpipe diagnostic` now writes a report even when compilation fails fatally —
  a syntax error or an unresolvable `load` — in both `text` and `json`. The
  report carries a diagnostic with `severity: "fatal"` and an empty `models`
  list, and the exit code stays 1, so tooling no longer has to parse stderr for
  the most common failure of a file being edited (#154).
- Composition operators are now commutative when unification merges elements
  of different kinds: a claim that one model argues (a sub-conclusion) and
  another asserts (an evidence) merges into a sub-conclusion whichever order
  the sources are listed in. `assemble(a, b)` and `assemble(b, a)` previously
  produced the same model only by luck, and the unlucky order failed to
  compile at all (#156).
- A unification group whose element kinds genuinely cannot be merged (e.g. the
  assembled conclusion colliding with a source evidence) is now reported as
  `incompatible-unification`, pointing at the operator call and naming both
  elements and their shared label, instead of surfacing as an unexplained
  `invalid-support` further down the build.

---

## [2.4.0] — 2026-08-10

### Added
- `jpipe diagnostic` accepts `-f/--format text|json`. The JSON report carries
  the same content as the human-readable one — diagnostics, statistics, model
  summary, symbol table, and executed actions — in a form tools can consume,
  making IDE integration possible without scraping the text output. The
  document declares a `schemaVersion` so consumers can detect its shape.
- Diagnostics now carry a machine-readable `code` (e.g. `unknown-model`,
  `no-duplicate-ids`) as a distinct field, exposed in the JSON report. Codes
  cover both compiler errors and consistency/completeness rules.
- A **published JSON Schema** documents the report as a stable contract for
  consumers. It ships on the classpath at
  `/schema/diagnostic-report-v1.schema.json`, is served from the documentation
  site, and is described in [the schema
  reference](https://ace-design.github.io/jpipe/design/diagnostic-schema/).
  Documents declare `schemaVersion` so consumers can branch on the shape.

### Changed
- The logo is suppressed automatically when `diagnostic -f json` writes to
  standard output, so the document is parseable without passing `--headless`.
  Text output and output to a file are unaffected.
- **For API consumers:** `Diagnostic.message()` no longer contains the
  `[code]` prefix — the code moved to `Diagnostic.code()`. The rendered text
  report is unchanged, but code reading `message()` programmatically should
  read `code()` instead. `DiagnosticCodes` constants lost their brackets
  accordingly (`"[unknown-model]"` → `"unknown-model"`).

### Fixed
- `diagnostic -f json` (and any future machine-readable output) no longer has
  its first bytes corrupted by the ASCII banner on standard output.

---

## [2.3.1] — 2026-08-07

### Fixed
- A glob pattern in a `load` directive may now point outside the declaring
  file's directory, e.g. `load "../library/*.jd" as lib`, or name an absolute
  location. Such patterns previously failed with a spurious `No file matches
  load pattern`, even though the equivalent literal path (`load
  "../library/foo.jd"`) worked. A pattern whose directory does not exist, or
  one that uses `..` after a wildcard (`*/../foo.jd`, which can never match),
  now reports a fatal error that names the actual problem (ADR-0022).

---

## [2.3.0] — 2026-08-06

### Added
- jPipe can now be installed on Windows with [Scoop](https://scoop.sh):
  `scoop bucket add mcscert https://github.com/jpipe-mcscert/scoop-mcscert`
  followed by `scoop install mcscert/jpipe`. Releases publish an additional
  `jpipe-<version>.zip` asset, and the release pipeline keeps the bucket
  manifest up to date automatically (#142, ADR-0025).

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

[Unreleased]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v4.0.0...HEAD
[4.0.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v3.0.0...v4.0.0
[3.0.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.5.0...v3.0.0
[2.5.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.4.0...v2.5.0
[2.4.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.3.1...v2.4.0
[2.3.1]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.3.0...v2.3.1
[2.3.0]: https://github.com/jpipe-mcscert/jpipe-compiler/compare/v2.2.0...v2.3.0
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
