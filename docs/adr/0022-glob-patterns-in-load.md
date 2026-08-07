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
*relative to the pattern's anchor directory* (see below):

| Pattern             | Matches                                                    |
|---------------------|------------------------------------------------------------|
| `models/*.jd`       | `.jd` files directly in `models/` (one level, no descent)  |
| `models/**/*.jd`    | `.jd` files in **sub**directories of `models/` only        |
| `models/**.jd`      | every `.jd` under `models/`, at any depth (incl. top level)|
| `../library/*.jd`   | `.jd` files directly in a `library/` directory *beside* the declaring file's directory |
| `/opt/models/*.jd`  | `.jd` files directly in an absolute location               |

### Anchoring: where the walk starts

A pattern is split at the last `/` preceding its **first wildcard character**.
Everything before that cut is a literal path, resolved against the declaring
file's directory exactly like a literal `load` — so it may contain `..` or be
absolute. The remainder is the glob, matched relative to the resolved directory,
which is also the only subtree that gets walked:

| Pattern            | Literal prefix | Anchor            | Effective glob |
|--------------------|----------------|-------------------|----------------|
| `models/*.jd`      | `models`       | `<dir>/models`    | `*.jd`         |
| `**.jd`            | *(none)*       | `<dir>`           | `**.jd`        |
| `../library/*.jd`  | `../library`   | `<dir>/../library`| `*.jd`         |
| `/opt/models/*.jd` | `/opt/models`  | `/opt/models`     | `*.jd`         |
| `{a,b}/*.jd`       | *(none)*       | `<dir>`           | `{a,b}/*.jd`   |

Anchoring is **meaning-preserving** for patterns that only descend: matching
`dir/X` against `dir/<glob>` relative to `<dir>` is equivalent to matching `X`
against `<glob>` relative to `<dir>/dir`, for every glob construct including
`**`. Because the cut is made before the first wildcard *character* rather than
at a segment boundary, a wildcard construct is never split — a brace group
spanning a separator such as `{foo/bar,baz}/*.jd` keeps its original meaning.

Without anchoring, a glob could only ever descend: a directory walk never
produces a path containing `..`, so `load "../library/*.jd"` matched nothing and
reported a spurious "no file matches", even though the literal
`load "../library/foo.jd"` resolved fine. Anchoring makes the two forms agree.

Two situations are rejected up front rather than surfacing as a confusing
no-match:

- a `..` segment **after** the first wildcard (e.g. `*/../foo.jd`) is
  unsatisfiable by a downward walk and is a FATAL;
- an anchor that is not an existing directory is a FATAL naming the directory
  (`Cannot expand load pattern '…': '…' is not a directory`), which points at
  the actual mistake — usually a typo in the literal prefix.

Glob *syntax* is validated before either check, so a malformed pattern is always
reported as such regardless of where it points.

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
  - for a glob, delegates to `expandGlob`.
- `expandGlob(…)` splits the pattern with `anchor(base, pattern)` into a
  `GlobAnchor(root, pattern)` record, compiles the `PathMatcher` (FATAL on a
  syntax error), rejects a post-wildcard `..` and a non-directory anchor, then
  matches via `matchGlob(root, matcher)`, FATALs on zero matches, and otherwise
  calls `expandOne` for each matched path and concatenates the results.
- `firstMetaChar(String)` is the single definition of what counts as a wildcard
  (`*`, `?`, `[`, `{`); both `isGlob` and `anchor` are derived from it so the
  literal/glob split and the anchor cut cannot drift apart.
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
- Globbing walks only the pattern's anchor subtree, not the whole subtree of the
  declaring file, so a pattern like `models/*.jd` never enumerates sibling
  directories. A pattern whose anchor is broad (`**.jd` at the top of a large
  tree) still walks everything below it; this is acceptable at the current
  project scale and can be optimized later with a `DirectoryStream` fast path
  for non-`**` patterns if needed.
- A pattern may now name files outside the project, via `..` or an absolute
  prefix. This is deliberate — it mirrors what a literal `load` path has always
  been able to do — and the usual consequence applies: a `.jd` file's set of
  dependencies is only as portable as the paths it names.
