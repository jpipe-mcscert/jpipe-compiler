# ADR-0007: Dependency Freshness Policy

**Date:** 2026-03-26
**Last revised:** 2026-11-22
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

Dependabot (`.github/dependabot.yml`) proposes updates **weekly**, as pull requests into `dev`, the repository's default branch ([ADR-0024](0024-git-branching-model.md)). It covers three ecosystems: Maven, through the version properties of the root `pom.xml`; GitHub Actions; and pip, for the Python tools the documentation site is built with.

- **Only stable releases are proposed.** Alpha, beta, milestone, release-candidate and snapshot versions are never offered while the project is on a stable version: Dependabot recognises these Maven qualifiers itself. A dependency that marks its pre-releases some other way (`-preview`, `-ea`) is given an explicit `ignore` entry.
- **A week's updates arrive in one pull request**, majors included: Maven, pip and GitHub Actions join one Dependabot multi-ecosystem group. A round of updates is one branch, verified as a whole, and merged as it arrives, once CI is green — not deferred to the next release. Dependencies that must match — the ANTLR runtime and its plugin (one `antlr4.version`), JUnit Jupiter and the JUnit Platform — need no group of their own: they always move in the same pull request.
- **A major is treated as a minor feature**, not a routine update: its migration happens on that week's branch, and an ADR is updated when the change affects conventions. A major that cannot be migrated that week is given a hold, with its reason, rather than holding the rest of the week back.
- **A major is held entirely** only where it is more than a bump, with the reason written beside the hold in `dependabot.yml`: ANTLR (a new major changes the generated parser), Log4j (Log4j 3 changes the configuration and plugin model the fat JAR is built around), and the dependencies Cucumber declares as version ranges (see *Constraints*).

All versions stay in `<properties>` in the root `pom.xml`, one property per version, so each update is a one-line, auditable diff.

### Documentation tooling

The documentation site is built by Python tools, installed by the `docs.yml` workflow. `docs/requirements.in` names them, and `docs/requirements.txt`, compiled from it by `pip-compile --generate-hashes`, locks the whole resolved set by version and hash, choosing only versions that ship wheels (`--pip-args='--only-binary :all:'`; the exact command is recorded in the lock's header, which Dependabot reuses). The workflow installs that lock with `--require-hashes --only-binary :all:`, so a build never installs anything it has not locked, and never runs a package's build code. The job that installs and builds holds a read-only token; the one that pushes the site to `gh-pages` runs no third-party code at all. Dependabot recompiles the lock. A pull request that changes the site's toolchain builds the site without deploying it, so a lock that cannot be installed fails there rather than after the merge.

The one exception is `jpipe-runner`, which `build.yml` installs by version range for the runner contract check: the runner co-evolves with the compiler, and that check exists to catch a new runner release breaking the generated step libraries, which a lock would hide.

### Release-time review

Before each release, the Maven Versions Plugin lists what the holds, and a weekly pull request not yet merged, keep back:

```
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates
```

The review decides, for each, whether it stays held for another cycle. It is a required step in the release checklist, not an optional housekeeping task.

### Security patches

Dependabot security updates are enabled on the repository. **Security patches** (CVEs rated High or Critical) are applied immediately on any branch and released as a `PATCH` version bump, regardless of the release cycle.

### Constraints

- **ANTLR4 runtime and plugin must be kept in sync** (`antlr4.version` drives both). A mismatch causes parse errors at runtime.
- **No version range is resolved at build time.** Resolving a range makes Maven download the artifact's version list from Maven Central on every build, and the build fails when that download does: a dependency that a library declares as a range is pinned in `<dependencyManagement>` to the version the range resolves to. Cucumber declares three (`jspecify`, `messages`, `query`). Their majors are held, because for each of them the narrowest range Cucumber declares stops below the next major, and the pins move by hand when a Cucumber update changes its ranges. `build.yml` runs `scripts/check-managed-ranges.sh`, which fails when a pin lies outside a range some dependency declares: Maven itself applies a pin silently.

## Rationale

- Weekly updates spread the work across the cycle instead of concentrating it on release day.
- One pull request a week is one branch to verify and merge, and dependencies that must match cannot be split across two. The cost is that a failing update fails the whole week, which jpipe-vscode ADR-VSC-0013 found with a grouped major on its first run: the failure is fixed on that branch, or the update is held, instead of reviewing a pull request per group every week.
- Holding a major only with a written reason keeps the list of holds short and each one re-examinable at release time.
- Targeting only LTS Java versions protects downstream projects (IDE plugins, CI integrations, operator authors) from being forced onto short-lived runtimes.
- Distinguishing security patches from routine updates lets the project respond to CVEs without waiting for a planned release.
- The cadence and the holds follow the other jPipe projects. This one departs from them in taking a week's updates as one pull request, and in keeping `SONAR_TOKEN` out of Dependabot's reach (see *Consequences*).

## Consequences

- Dependabot's pull requests run `build.yml` like any other, but get no SonarCloud analysis: GitHub withholds the repository's secrets from the runs Dependabot triggers, so `sonar.yml` skips its job there, as it does for a pull request from a fork. The change is analysed on the push that merges it into `dev`. jpipe-vscode (ADR-VSC-0013) copies `SONAR_TOKEN` into Dependabot's own secret store; this project does not, on purpose: `mvn verify` runs the build plugins a bump brings, and a compromised release would then run with the token.
- Vulnerability alerts and Dependabot security updates must be enabled in the repository settings; `dependabot.yml` configures version updates only.
- The release checklist keeps a `mvn versions:display-dependency-updates` run, to review what is held back rather than to perform the updates.
- When Java 29 LTS is published, a PR bumping `maven.compiler.release` to 29 becomes a prerequisite for the next minor release. Dependabot does not propose it: the Java release is not a dependency.
- A new hold is added to `dependabot.yml` with its reason, not decided case by case by closing pull requests.
