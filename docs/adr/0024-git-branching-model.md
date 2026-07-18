# ADR-0024: Git Branching Model — `dev` for Integration, `main` for Releases

**Date:** 2026-07-18
**Status:** Accepted

## Context

Until now the repository used a single long-lived branch, `main`. Feature work,
bug fixes, chores, and release-preparation commits all landed directly on
`main`, interleaved. Two forces make this arrangement increasingly awkward:

- **Releases are tag-driven.** [ADR-0020](0020-tag-triggered-release-pipeline.md)
  publishes a stable release only when a `v*.*.*` tag is pushed, and
  `release.yml` already verifies that the tag points at a commit on `main`.
  Nothing else about `main` distinguishes "released" commits from
  "work-in-progress" commits — the branch is a mix of both.
- **`main` also drives the rolling `unstable` snapshot.** The `publish-unstable`
  job in `build.yml` re-points the `unstable` pre-release on every push to
  `main`. So `main` is simultaneously the integration branch *and* the release
  branch, and the `CHANGELOG.md` `[Unreleased]` section accumulates directly on
  the same branch that is supposed to represent shipped software.

There is no branch whose state answers the question "what is the latest
*released* version?" without inspecting tags, and no branch that represents
"everything merged since the last release" other than `main` itself.

## Decision

Adopt a two-tier long-lived branch model.

- **`main` is release-only.** Every commit on `main` corresponds to a released,
  tagged version. `main` moves *only* when a release is cut. Its `HEAD` always
  equals the most recent published release. No feature or fix is committed to
  `main` directly.
- **`dev` is the integration branch.** It is the default target for day-to-day
  work and always contains "everything merged since the last release." The
  `CHANGELOG.md` `[Unreleased]` section lives and grows on `dev`.
- **Feature branches** (`feat/…`, `fix/…`, `chore/…`, `docs/…`) branch **from
  `dev`** and merge back **into `dev`**, normally via pull request. Merging a
  branch into `dev` is what adds its entry to `[Unreleased]`.
- **Releasing is `dev` → `main` + tag.** To release, `dev` is merged into
  `main`, the release-preparation commit renames `[Unreleased]` to
  `## [X.Y.Z] — YYYY-MM-DD` (see the changelog policy in `CLAUDE.md`), and a
  `vX.Y.Z` tag is pushed on `main`. The tag triggers `release.yml`
  (ADR-0020). After the release, `main` is merged back into `dev` so the two
  branches share the release commit.
- **Hotfixes** for an already-released version branch from `main` (`fix/…`),
  merge into `main`, are released via a patch tag, and are then merged back into
  `dev`.

Version numbering (semantic versioning, [ADR-0001](0001-semver-versioning.md))
and the tag-triggered pipeline (ADR-0020) are unchanged; this ADR only changes
*which branch* carries integration versus release state.

### CI consequences (applied in this change)

- **`build.yml` `publish-unstable`** now triggers on push to **`dev`** instead
  of `main` — the rolling snapshot tracks the integration branch, which is where
  continuous merges now happen. (`main` would otherwise only move at release
  time, making the "unstable" snapshot stale.)
- **`sonar.yml`** runs its push analysis on both `main` and `dev` so the
  integration branch is quality-gated.
- **`docs.yml`** deploys documentation from **`dev`**, so ADRs and design docs
  publish when they are integrated rather than only at release time.
- **`release.yml`** is unchanged: it still triggers on `v*.*.*` tags and still
  requires the tag to be an ancestor of `main`.

## Rationale

- **`main` becomes a truthful release ledger.** `git log main` is exactly the
  release history; `main@HEAD` is exactly what users get from Homebrew / the PPA.
  This mirrors the intent already encoded in `release.yml`'s "tag must be on
  main" check, and makes it structural rather than incidental.
- **`[Unreleased]` gets a natural home.** The changelog's unreleased section is,
  by definition, "merged but not released" — precisely the contents of `dev`.
  Keeping it off `main` removes the oddity of a shipped branch carrying an
  `[Unreleased]` heading between releases.
- **Deliberate releases, continuous integration.** Work still flows
  continuously (into `dev`, with an `unstable` snapshot), while releasing stays
  an explicit, low-frequency act (a `dev` → `main` merge plus a tag), consistent
  with ADR-0020's "releases are deliberate" rationale.
- **Low ceremony.** Two long-lived branches and short-lived topic branches is
  the smallest model that separates integration from release; it does not
  require release branches or a full git-flow.

## Consequences

- A `dev` branch is created from the current `main` and becomes the default
  branch for development; branch protection should require PRs into `dev` and
  forbid direct pushes to `main`.
- Contributors branch from `dev`, not `main`. The default branch on the remote
  should be set to `dev` so clones and PRs target it by default.
- The release checklist gains a first step: merge `dev` into `main` before
  tagging (and merge `main` back into `dev` afterwards). The existing
  ADR-0020 / ADR-0023 release steps follow unchanged.
- The `unstable` GitHub pre-release now reflects `dev`. Its notes ("rebuilt on
  every push") are updated to reference `dev`.
- Published documentation tracks `dev`; a doc fix is visible once merged, not
  only after the next release.
- Existing open work branched from `main` (e.g. the branch introducing this
  ADR) is retargeted onto `dev`.
