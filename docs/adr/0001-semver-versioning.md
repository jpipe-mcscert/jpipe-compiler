# ADR-0001: Semantic Versioning

**Date:** 2026-03-25
**Status:** Accepted

## Context

jPipe is distributed through multiple channels: Maven Central, Homebrew, and Canonical PPA. The project also exposes a public extension API (`jpipe-operators`) that external users can implement to define custom composition operators.

A calendar-based versioning scheme (CalVer, e.g. `year.month.patch` as used by Ubuntu) was considered, as it communicates the currency of a release and fits naturally with the apt/PPA distribution channel.

## Decision

Use [Semantic Versioning](https://semver.org) (`MAJOR.MINOR.PATCH`).

## Rationale

- Maven Central tooling (Dependabot, version range resolution, compatibility checks) assumes SemVer semantics.
- `jpipe-operators` is a public API — external projects implement it to define custom operators. A major version bump on breaking changes gives downstream users an unambiguous compatibility signal that CalVer cannot provide.
- CalVer is valid on Maven Central but unconventional in the Java ecosystem, adding friction for contributors and users.

## Consequences

- Breaking changes to any public API (especially `jpipe-operators`) require a `MAJOR` version bump.
- Additive, backwards-compatible changes increment `MINOR`.
- Bug fixes increment `PATCH`.
- Pre-release versions use the `-SNAPSHOT` suffix during development, per Maven convention.
