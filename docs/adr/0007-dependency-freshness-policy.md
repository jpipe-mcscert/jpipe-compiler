# ADR-0007: Dependency Freshness Policy

**Date:** 2026-03-26
**Status:** Accepted

## Context

jPipe is distributed through Maven Central and consumed by downstream projects that implement custom operators via `jpipe-operators`. Stale dependencies accumulate security vulnerabilities and compatibility debt; but chasing every minor release mid-cycle destabilises the build and disrupts downstream consumers.

Two tensions must be balanced:

- **Java version** — non-LTS Java releases receive only six months of support. Targeting a non-LTS version locks downstream users into a short-lived runtime. LTS releases (8, 11, 17, 21, 25, …) receive multi-year support and are the practical baseline for production deployments.
- **Library versions** — always running latest gives the most security patches and API improvements, but introduces breaking changes unpredictably. Never updating accumulates CVEs and misses performance fixes.


## Decision

### Java version

The project targets the **current Java LTS release**. When a new LTS is published (every two years, at major versions 17 → 21 → 25 → 29 → …), `maven.compiler.release` is updated as part of the next jPipe release. Non-LTS versions are never targeted.

### Library and plugin versions

All dependency and plugin versions are reviewed and updated to the latest stable release **before each jPipe release**. This review is a required step in the release checklist, not an optional housekeeping task.

The Maven Versions Plugin is the canonical tool for this review:

```
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates
```

Updates are applied in `<properties>` in the root `pom.xml`. Each version property is updated individually so the diff is auditable.

### Constraints

- **ANTLR4 runtime and plugin must be kept in sync** (`antlr4.version` drives both). A mismatch causes parse errors at runtime.
- **Major version bumps** (e.g. Log4j 2→3, JUnit 5→6) are treated as minor features, not routine updates: they require a dedicated branch, migration work, and a corresponding ADR amendment if the change affects conventions.
- **Security patches** (CVEs rated High or Critical) are applied immediately on any branch and released as a `PATCH` version bump, regardless of the release cycle.

## Rationale

- Reviewing at release time, not continuously, bounds the noise while ensuring no release ships with versions that are more than one cycle old.
- Targeting only LTS Java versions protects downstream projects (IDE plugins, CI integrations, operator authors) from being forced onto short-lived runtimes.
- Centralising all versions in root `pom.xml` `<properties>` makes the review mechanical: one file, one diff, one PR.
- Distinguishing security patches from routine updates lets the project respond to CVEs without waiting for a planned release.

## Consequences

- The release checklist must include a `mvn versions:display-dependency-updates` run and a commit updating all stale versions.
- Dependabot or Renovate PRs that target non-security updates should be deferred to the next planned release rather than merged immediately.
- When Java 29 LTS is published, a PR bumping `maven.compiler.release` to 29 becomes a prerequisite for the next minor release.
- Major-version upgrades of dependencies (ANTLR5 when it ships, etc.) require their own ADR amendment documenting migration impact.
