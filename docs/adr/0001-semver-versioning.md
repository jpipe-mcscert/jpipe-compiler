# ADR-0001: Semantic Versioning

**Date:** 2026-03-25
**Status:** Accepted

## Context

jPipe is distributed as a command-line tool: the release pipeline (ADR-0020)
publishes a fat JAR on each GitHub Release, and the Homebrew tap, the Ubuntu PPA
and the Scoop bucket install it with a `jpipe` launcher (ADR-0025). The Java
modules (`jpipe-lang`, `jpipe-model`, `jpipe-operators`, `jpipe-compiler`,
`jpipe-cli`) are not published to any Maven repository, and nothing outside this
repository compiles against them. Operators are not an extension point either:
`CompilerFactory.builtInOperators()` registers the built-in ones, and a
service-loader mechanism is left to a later ADR (ADR-0018).

What other people and tools depend on is therefore the CLI and what goes in and
out of it:

- the jPipe language, as `.jd` files written by users;
- the `jpipe` commands, their options, and their exit codes (`docs/design/cli.md`);
- the export formats, which other tools read: the `PYTHON` step-library skeleton
  and the `JSON` model, which jpipe-runner consumes (`scripts/runner-contract.sh`
  checks that contract), as well as `JPIPE`, `DOT` and the image formats;
- the diagnostics: the `file:line:column: severity: [code] message` lines on
  stderr, which jpipe-vscode and scripts parse, and the `diagnostic` command's
  JSON report, whose schema the site publishes.

A calendar-based versioning scheme (CalVer, e.g. `year.month.patch` as used by
Ubuntu) was considered, as it communicates the currency of a release and fits
naturally with the apt/PPA distribution channel.

## Decision

Use [Semantic Versioning](https://semver.org) (`MAJOR.MINOR.PATCH`), measured
against the CLI's public interface listed above.

- **MAJOR:** a change that can break a user or a tool that relies on that
  interface. A `.jd` file that compiled stops compiling or changes meaning; a
  command or an option is removed or renamed, or an exit code changes meaning;
  an export format or the diagnostic output changes in a way its readers must
  adapt to.
- **MINOR:** new behaviour that keeps every existing use working: new syntax,
  a new command, option, export format or diagnostic, or a field added to a
  JSON output.
- **PATCH:** bug fixes only.

The Java modules are internal. Their classes and methods change as the code
needs, whatever their Java visibility, with no deprecation period and no effect
on the version number. A change to them that nothing outside the code can
observe needs no CHANGELOG entry either.

Publishing the modules, or opening an extension point such as third-party
operators, would make the published API part of the public interface. This ADR
is then rewritten to cover it first, since from that release on, removing a
public class or method requires a MAJOR bump.

## Rationale

- Users and tools depend on the CLI, not on Java signatures. Versioning the
  interface they actually use gives them an unambiguous signal: a MAJOR bump is
  the one release that may need action on their side.
- Treating the modules as internal keeps refactoring cheap. Unused public API
  can be deleted rather than carried through a deprecation cycle that no one
  outside the repository would ever see, and a refactoring across modules is not
  held back to the next major release.
- Semantic versioning remains the convention of the Java ecosystem, and of the
  package managers jPipe ships through; CalVer would add friction for
  contributors and users without telling them whether an upgrade is safe.

## Consequences

- Choosing the version number at release time (`docs/releasing.md`, Phase 0)
  reads the `[Unreleased]` section of the CHANGELOG against the list in the
  Context section.
- A change to the `PYTHON` or `JSON` export is checked against jpipe-runner's
  contract test before it is called MINOR.
- Pre-release versions use the `-SNAPSHOT` suffix during development, per Maven
  convention.
