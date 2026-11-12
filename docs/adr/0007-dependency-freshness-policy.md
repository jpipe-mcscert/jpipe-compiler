# ADR-0007: Dependency Freshness Policy

**Date:** 2026-03-26
**Last revised:** 2026-11-10
**Status:** Accepted

## Context

jPipe is distributed through Maven Central and consumed by downstream projects that implement custom operators via `jpipe-operators`. Stale dependencies accumulate security vulnerabilities and compatibility debt; but taking a breaking release by surprise destabilises the build and disrupts downstream consumers.

Two tensions must be balanced:

- **Java version** — non-LTS Java releases receive only six months of support. Targeting a non-LTS version locks downstream users into a short-lived runtime. LTS releases (8, 11, 17, 21, 25, …) receive multi-year support and are the practical baseline for production deployments.
- **Library versions** — always running latest gives the most security patches and API improvements, but introduces breaking changes unpredictably. Never updating accumulates CVEs and misses performance fixes.

A review held only at release time lets a whole cycle of updates pile up into one release-day diff, and covers nothing the Maven Versions Plugin cannot see — the GitHub Actions the workflows run, for one. The other jPipe projects take weekly, grouped updates from Dependabot: the VS Code extension (jpipe-vscode ADR-VSC-0013) and the runner (jpipe-runner ADR-0026).

## Decision

### Java version

The project targets the **current Java LTS release**. When a new LTS is published (every two years, at major versions 17 → 21 → 25 → 29 → …), `maven.compiler.release` is updated as part of the next jPipe release. Non-LTS versions are never targeted.

### Continuous updates

Dependabot (`.github/dependabot.yml`) proposes updates **weekly**, as pull requests into `dev`, the repository's default branch ([ADR-0024](0024-git-branching-model.md)). It covers two ecosystems: Maven, through the version properties of the root `pom.xml`, and GitHub Actions.

- **Only stable releases are proposed.** Alpha, beta, milestone, release-candidate and snapshot versions are never offered while the project is on a stable version: Dependabot recognises these Maven qualifiers itself. A dependency that marks its pre-releases some other way (`-preview`, `-ea`) is given an explicit `ignore` entry.
- **Minor and patch updates arrive grouped**: one pull request for libraries, one for build plugins, so a red build names one week's changes. They are merged as they arrive, once CI and the quality gate are green — not deferred to the next release.
- **GitHub Actions arrive in one pull request, majors included.** The workflows pin each action by major tag, so every update is a major, and they are mostly runtime moves.
- **Dependencies that must match move in one group, majors included**: the ANTLR runtime and its plugin (one `antlr4.version`), and JUnit Jupiter with the JUnit Platform.
- **A date-versioned dependency** (`org.json`, versioned `YYYYMMDD`) has its own group that accepts every update, since each of its releases is a semver major.
- **A major arrives as its own pull request** and is treated as a minor feature, not a routine update: the migration happens on that branch, and an ADR is updated when the change affects conventions.
- **A major is held entirely** only where it is more than a bump, with the reason written beside the hold in `dependabot.yml`: ANTLR (a new major changes the generated parser) and Log4j (Log4j 3 changes the configuration and plugin model the fat JAR is built around).

All versions stay in `<properties>` in the root `pom.xml`, one property per version, so each update is a one-line, auditable diff.

### Release-time review

Before each release, the Maven Versions Plugin lists what the holds and the unmerged majors keep back:

```
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates
```

The review decides, for each, whether it stays held for another cycle. It is a required step in the release checklist, not an optional housekeeping task.

### Security patches

Dependabot security updates are enabled on the repository. **Security patches** (CVEs rated High or Critical) are applied immediately on any branch and released as a `PATCH` version bump, regardless of the release cycle.

### Constraints

- **ANTLR4 runtime and plugin must be kept in sync** (`antlr4.version` drives both). A mismatch causes parse errors at runtime.

## Rationale

- Weekly, grouped updates keep each change small and attributable, and spread the work across the cycle instead of concentrating it on release day.
- Groups carry minor and patch only: a grouped major fails the whole group, which jpipe-vscode ADR-VSC-0013 found on its first run. A major on its own pull request is the dedicated branch its migration needs.
- Holding a major only with a written reason keeps the list of holds short and each one re-examinable at release time.
- Targeting only LTS Java versions protects downstream projects (IDE plugins, CI integrations, operator authors) from being forced onto short-lived runtimes.
- Distinguishing security patches from routine updates lets the project respond to CVEs without waiting for a planned release.
- The same policy across the jPipe projects means one way of handling an update pull request, whichever repository it lands in.

## Consequences

- Dependabot's pull requests run `build.yml` and `sonar.yml` like any other, and need `SONAR_TOKEN` in Dependabot's own secret store to pass the SonarCloud gate, as jpipe-vscode ADR-VSC-0013 found.
- Vulnerability alerts and Dependabot security updates must be enabled in the repository settings; `dependabot.yml` configures version updates only.
- The release checklist keeps a `mvn versions:display-dependency-updates` run, to review what is held back rather than to perform the updates.
- When Java 29 LTS is published, a PR bumping `maven.compiler.release` to 29 becomes a prerequisite for the next minor release. Dependabot does not propose it: the Java release is not a dependency.
- A new hold is added to `dependabot.yml` with its reason, not decided case by case by closing pull requests.
