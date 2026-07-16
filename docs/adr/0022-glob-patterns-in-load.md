# ADR-0022: Glob Patterns in the `load` Directive

**Date:** 2026-07-15
**Status:** Accepted

## Context

ADR-0017 introduced `load` as a compiler step (`LoadResolver`) that expands a
single `load "path" as ns` directive into the commands of the referenced file.
Each `load` imports exactly **one** file. Projects that split their models
across many `.jd` files must enumerate every file with its own `load` line:

```
load "models/a.jd" as lib
load "models/b.jd" as lib
load "models/c.jd" as lib
```

This is verbose and easy to forget to update when a file is added. We want a
single directive to pull in a whole set of files matched by a pattern, e.g.
`load "models/*.jd" as lib`.

Two questions drove the design:

1. **Does this need a new grammar construct?**
2. **What matching syntax — regular expressions or globs?**

## Decision

### Same `load` command — no grammar change

The `load` rule is already pattern-agnostic:

```antlr
load   : LOAD path=STRING (AS namespace=ID)?;
STRING : '"' STRING_CHAR* '"' | '\'' STRING_CHAR* '\'' ; // any char except CR/LF
```

`path` is a free-form `STRING`, so `load "models/*.jd"` is *already*
syntactically valid. We keep the same `load` production — no new keyword, no new
rule, no lexer change. This extends ADR-0017's model naturally: `load` was
already a macro expanding one directive into many *commands*; it now expands one
directive into many *files* worth of commands.

### Glob syntax (not regex)

Matching uses **glob** patterns via Java's native
`FileSystems.getDefault().getPathMatcher("glob:" + pattern)`. Globs are the
idiomatic, familiar way to match files, are safe, and require no manual tree
walking beyond a `Files.walk`. True regular expressions on file paths are
unusual and were rejected.

Standard Java NIO glob semantics apply, matched against each candidate's path
*relative to the declaring file's directory*:

| Pattern         | Matches                                                    |
|-----------------|------------------------------------------------------------|
| `models/*.jd`   | `.jd` files directly in `models/` (one level, no descent)  |
| `models/**/*.jd`| `.jd` files in **sub**directories of `models/` only        |
| `models/**.jd`  | every `.jd` under `models/`, at any depth (incl. top level)|

### Backward compatibility

A path with no glob metacharacters (`*`, `?`, `[`, `{`) is treated as a literal
path and takes exactly the historical single-file code path — including the
`Cannot open loaded file` FATAL when the file is missing. Existing `load`
directives are unaffected.

### Namespace on a multi-match: shared

When a pattern matches several files and an `as ns` alias is given, **all**
matched files' models are imported under the single namespace `ns`. Two matched
files that declare a model with the same name collide at interpretation time —
identical to flat-importing two files that share a name today (ADR-0017). No
per-file sub-namespace is derived.

### Zero matches: FATAL

A pattern that matches no file is almost always a typo or a wrong path, so it is
reported as a FATAL diagnostic (`No file matches load pattern '<pattern>'`),
consistent with the existing behavior for a missing literal path.

### Deterministic order

Matched paths are sorted lexicographically before expansion, so compilation
output does not depend on filesystem enumeration order.

## Implementation

The change is localized to `LoadResolver`
(`ca.mcscert.jpipe.compiler.steps.transformations.LoadResolver`):

- `expand(LoadDirective, …)` now computes the base directory, and:
  - for a literal path, calls the extracted per-file helper `expandOne` once;
  - for a glob, matches via `matchGlob`, FATALs on zero matches, otherwise calls
    `expandOne` for each matched path and concatenates the results.
- `expandOne(Path, …)` holds the unchanged per-file body from ADR-0017 (cycle
  detection via the `visited` set, duplicate suppression via the `loaded` set,
  sub-file parsing, recursive resolution, namespace prefixing, diagnostic
  forwarding). Because these checks are per file, they work identically for a
  literal load and for every match of a glob.

`ActionListProvider.enterLoad` is unchanged: it still emits a single
`LoadDirective(pattern, namespace)`; the `path` field now simply may hold a
glob.

## Consequences

- Cycle detection and idempotent-load suppression apply per matched file, so a
  glob that (transitively) re-includes a file already in progress is still
  flagged as a cycle, and a file matched twice under the same namespace is
  loaded once.
- Because Java glob is used verbatim, `**/*.jd` matches nested files **only**;
  `**.jd` is the pattern for "every `.jd` at any depth". This is documented in
  the table above and in `examples/022_load_glob_recursive.jd`.
- A shared namespace over many files can surface name collisions at
  interpretation time; this is intentional and matches existing flat-import
  semantics.
- Globbing walks the declaring file's directory subtree. For very large trees a
  single-level `*.jd` still walks the whole subtree and filters; this is
  acceptable at the current project scale and can be optimized later with a
  `DirectoryStream` fast path for non-`**` patterns if needed.
