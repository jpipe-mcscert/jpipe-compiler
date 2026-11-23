# ADR-0024: Git Branching Model — `dev` for Integration, `main` for Releases

**Date:** 2026-07-18 (last revised 2026-11-23)
**Status:** Accepted

## Context

The repository used to have a single long-lived branch, `main`. Feature work,
bug fixes, chores, and release-preparation commits all landed directly on
`main`, interleaved. Two forces made this arrangement awkward:

- **Releases are tag-driven.** [ADR-0020](0020-tag-triggered-release-pipeline.md)
  publishes a stable release only when a `v*.*.*` tag is pushed, and
  `release.yml` verifies that the tag points at a commit on `main`. Nothing else
  about `main` distinguished "released" commits from "work-in-progress" commits:
  the branch mixed both.
- **`main` also drove a rolling `unstable` snapshot.** A job in `build.yml`
  re-pointed an `unstable` pre-release on every push to `main`. So `main` was
  at once the integration branch *and* the release branch, and the
  `CHANGELOG.md` `[Unreleased]` section accumulated on the branch that was
  supposed to represent shipped software.

No branch answered the question "what is the latest *released* version?"
without inspecting tags, and no branch represented "everything merged since the
last release" other than `main` itself.

## Decision

Adopt a two-tier long-lived branch model.

- **`main` is release-only.** Every commit on `main` corresponds to a released,
  tagged version. `main` moves *only* when a release or a hotfix is published,
  and its `HEAD` always equals the most recent published release. It is entered
  only through a pull request (see [Releases](#releases) and
  [Hotfixes](#hotfixes)), and the `vX.Y.Z` tag is pushed by a human.
- **`dev` is the integration branch.** It is the default branch on the remote,
  the target of day-to-day work, and always contains "everything merged since
  the last release." The `CHANGELOG.md` `[Unreleased]` section lives and grows
  on `dev`.
- **Topic branches** (`feat/…`, `fix/…`, `chore/…`, `docs/…`) branch **from
  `dev`** and merge back **into `dev`**. Merging a branch into `dev` is what
  adds its entry to `[Unreleased]`.

Version numbering (semantic versioning, [ADR-0001](0001-semver-versioning.md))
and the tag-triggered pipeline (ADR-0020) are unchanged; this ADR decides
*which branch* carries integration versus release state.

### How a change enters `dev`

- **A pull request is the default** for any change with reviewable content: new
  behaviour, bug fixes of substance, grammar or model changes, anything a
  second reader would improve.
- **A direct push to `dev` is permitted** when the author judges the change
  low-risk and `mvn verify` is green locally. Examples:
    - release bookkeeping, such as the version bump to the next `-SNAPSHOT`
      after a release;
    - CI and build configuration;
    - small fixes carried by their own tests.

### Releases

- **A release is one pull request, from a release branch into `main`.**
  `release/X.Y.Z` is cut from the current `dev`, so it holds everything `dev`
  holds. `scripts/release.sh prepare X.Y.Z` commits the version and the closed
  changelog there (`[Unreleased]` becomes `## [X.Y.Z] — YYYY-MM-DD`), and
  refuses to run on any other branch. The release pull request's diff is the
  whole release.
- **After the merge, a human pushes the `vX.Y.Z` tag on `main`**, which
  triggers `release.yml` (ADR-0020).
- **`main` is then merged back into `dev`**, which brings the release commit to
  `dev`: it does not reach `dev` before the release. The version bump to the
  next `-SNAPSHOT` follows, pushed straight to `dev`.

The step-by-step runbook is [`docs/releasing.md`](../releasing.md); the
organization's rule is in its
[contributing guide](https://github.com/jpipe-mcscert/.github/blob/main/CONTRIBUTING.md#releases).

### Hotfixes

A hotfix for an already-released version branches from `main` (`fix/…`), merges
into `main` through a pull request, is released via a patch tag, and is then
merged back into `dev`.

### CI and branch protection

- **No `unstable` pre-release.** The tip of `dev` *is* the rolling "latest
  integrated" state, so the `unstable` GitHub pre-release and its force-pushed
  `unstable` tag, introduced by
  [ADR-0020](0020-tag-triggered-release-pipeline.md), are retired. Per-run fat-JAR
  artifacts remain available on each Actions run.
- **`build.yml`** has no branch filter: it runs on every push and pull request.
- **`sonar.yml`** runs its push analysis on both `main` and `dev`, so the
  integration branch is quality-gated.
- **`docs.yml`** deploys documentation from **`dev`**, so ADRs and design docs
  publish when they are integrated rather than only at release time.
- **`release.yml`** triggers on `v*.*.*` tags and requires the tag to be an
  ancestor of `main`.
- **Branch protection** on `main` requires a pull request. On `dev` it forbids
  force-pushes and requires the status checks to pass, but must **not** require
  a pull request.

## Rationale

- **`main` is a truthful release ledger.** `git log main` is exactly the
  release history; `main@HEAD` is exactly what users get from Homebrew, Scoop
  and the PPA. This mirrors the intent encoded in `release.yml`'s "tag must be
  on main" check, and makes it structural rather than incidental.
- **`[Unreleased]` has a natural home.** The changelog's unreleased section is,
  by definition, "merged but not released": precisely the contents of `dev`.
  Keeping it off `main` removes the oddity of a shipped branch carrying an
  `[Unreleased]` heading between releases.
- **Deliberate releases, continuous integration.** Work flows continuously into
  `dev`, whose `HEAD` is the latest integrated code, while releasing stays an
  explicit, low-frequency act, consistent with ADR-0020's "releases are
  deliberate" rationale.
- **Review where it pays.** The pull request was never what made a change to
  `dev` safe: `build.yml` and `sonar.yml` build, test and quality-gate a direct
  push exactly like a pull-requested one. What the pull request adds is
  *review*, valuable for reviewable content and pure overhead for a version
  bump.
- **One review per release.** Every change in a release branch was reviewed on
  its way into `dev`, and the release pull request reviews and gates the
  release as a whole. Sending the release commit through `dev` first would take
  a second step that re-runs every check on what `dev` already reviewed.
  Everything in `main` is in `dev` once `main` is merged back.
- **Low ceremony.** Two long-lived branches, short-lived topic branches, and a
  release branch that lives for one pull request is the smallest model that
  separates integration from release; it does not need a full git-flow.

## Consequences

- Contributors branch from `dev`, not `main`; `dev` is the default branch on
  the remote, so clones and pull requests target it.
- `docs.yml` deploys on every push to `dev`, so a docs change pushed directly is
  published immediately, with no staging step. Authors weigh that before
  skipping the pull request.
- Work merged into `dev` while a release pull request is open waits for the
  next release.
- Users who want the latest build take the tip of `dev` or a per-run Actions
  artifact.
- Published documentation tracks `dev`: a doc fix is visible once merged, not
  only after the next release.
- ADR-0020's release procedure sets the release version during preparation, on
  the release branch.
