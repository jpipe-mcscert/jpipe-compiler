# ADR-0017: Load Directive Resolved as a Compiler Step

**Date:** 2026-04-04
**Status:** Accepted

## Context

The jPipe grammar allows a source file to import models from another file:

```
load "path/to/other.jd" as namespace
load "path/to/other.jd"          // namespace-less variant
```

The loaded file's models are made available under the given namespace alias (or
flat, without a prefix, when `as` is omitted). Loads are recursive: a loaded
file may itself load further files. Paths are resolved relative to the file that
declares the `load`, not to the calling file.

In the previous compiler architecture, `load` was implemented as a
`MacroCommand` inside `jpipe-model`: at execution time the engine expanded it
into the commands produced by parsing the referenced file. That design required
`jpipe-model` to call back into `jpipe-compiler`, creating a circular module
dependency that cannot exist in the current multi-module build.

Two alternative approaches were considered:

- **Grammar pre-processing** — differentiate `load` syntactically (e.g., a
  `#load` preprocessor directive) and resolve it in a text-substitution pass
  before ANTLR runs. Avoids touching the domain model, but requires a separate
  lexer phase, makes error locations harder to track, and loses the clean grammar
  rule.
- **Compile-and-merge** — compile each loaded file independently into a `Unit`,
  then merge the resulting `Unit` objects before validation. Keeps each file's
  compilation self-contained, but requires adding a merge API to `Unit`, forces
  a decision about whether validation runs per-unit or on the merged whole, and
  complicates source-location propagation across merge boundaries.

## Decision

`load` is resolved as a dedicated `Transformation` step (`LoadResolver`) that
runs inside `jpipe-compiler`, between `ActionListProvider` and
`ActionListInterpretation`:

```
… → ActionListProvider → LoadResolver → ActionListInterpretation → …
```

`ActionListProvider` produces a `LoadCommand` record (defined in
`jpipe-compiler`, not `jpipe-model`) whenever it encounters a `load` directive.
`LoadCommand` implements `Command` so it can inhabit the `List<Command>` that
flows between the two steps, but its `condition()` always returns `false` and
its `execute()` always throws, preventing it from reaching `ExecutionEngine`.

`LoadResolver` eliminates every `LoadCommand` by:

1. Resolving the path to an absolute, normalised `Path` relative to the
   directory of the file currently being compiled (`ctx.sourcePath()`).
2. Detecting load cycles: a `Set<Path>` of in-progress files is threaded through
   the recursion. If the resolved path is already in the set, a `FATAL`
   diagnostic is reported and the load is skipped.
3. Parsing the referenced file through a raw sub-chain (the same steps as
   `parsingChain()`, but without `LoadResolver` itself) using a fresh
   `CompilationContext` bound to the sub-file.
4. Recursively resolving any `LoadCommand`s found in the sub-file's command list,
   passing an extended copy of the visited set.
5. Prefixing every model-name argument of every command in the expanded list
   with `namespace + ":"` via `CommandPrefixer`, when a namespace alias was
   given. Element IDs and display labels are not prefixed; they are local to
   their model and get qualified by `JustificationModel.inline()` at template
   expansion time.
6. Splicing the resulting flat list in place of the `LoadCommand`.

Diagnostics produced while compiling a sub-file are forwarded to the parent
`CompilationContext` via a `finally` block, so the root caller receives a
unified error report. The visited-set is copied (not shared) on each branch so
that a file loaded from two independent paths is not incorrectly flagged as a
cycle.

When `as` is omitted, step 5 is skipped: the sub-file's models are imported
flat, under their declared names, into the parent unit.

## Rationale

- **No circular module dependency.** `LoadCommand` and `LoadResolver` live in
  `jpipe-compiler`; `jpipe-model` remains unaware of file loading. The
  dependency direction (`compiler → model`, never `model → compiler`) is
  preserved.
- **The `ExecutionEngine` sees a flat, fully expanded `List<Command>`.** It
  needs no knowledge of `load` at all. This is the same principle as
  `MacroCommand` — eliminating a directive before interpretation — but applied
  one layer earlier, at the pipeline level rather than the command level.
- **Command prefixing is the right granularity.** Namespace isolation is a
  compilation concern, not a domain-model concern. Rewriting model-name strings
  in commands before they are executed means the `Unit` ends up with correctly
  namespaced models without any change to the model API.
- **Cycle detection is O(depth) per file.** Passing an immutable snapshot of the
  visited set down each branch makes cycle detection both correct (a file
  reachable via two independent paths is not a cycle) and cheap (one set lookup
  per load directive).
- **Grammar-pre-processing is avoided.** The `load` rule remains a first-class
  grammar production, so ANTLR reports syntax errors at accurate source
  locations.
- **Compile-and-merge is avoided.** Merging `Unit` objects would require
  duplicating validation logic or deferring it to the merged whole. Merging at
  the command-list level instead means validation always runs on the single
  final `Unit`, with no special handling needed.

## Consequences

- `LoadCommand` must never reach `ExecutionEngine`. Its `condition() = false`
  and `execute() = throw` enforce this by construction, but `LoadResolver` must
  always appear in the pipeline between `ActionListProvider` and
  `ActionListInterpretation`.
- Model names from a loaded file are prefixed, but element IDs are not.
  Overriding an abstract support that was inherited through a load requires the
  full qualified key: `namespace:templateName:elementId`. This is consistent
  with the existing override syntax (ADR-0012) and documented in the examples.
- Omitting `as` imports all models from the loaded file into the current
  namespace without prefix. If two loaded files declare models with the same
  name, `ExecutionEngine` will report a duplicate-model error at interpretation
  time, not at load time.
- Cycle detection operates on normalised absolute file paths. Symlinks that
  resolve to the same inode via different paths will be treated as the same file
  and correctly flagged as a cycle.
- Sub-file diagnostics (errors, fatals) are forwarded to the root context. A
  `FATAL` in any sub-file aborts the entire pipeline via the standard fast-fail
  mechanism in `Transformation.fire()`.
