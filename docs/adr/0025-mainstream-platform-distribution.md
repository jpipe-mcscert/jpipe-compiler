# ADR-0025: Mainstream Platform Distribution Policy

**Date:** 2026-08-06
**Status:** Accepted

## Context

jPipe reaches its end users through operating-system package managers, not
through a download page. The set of channels grew ad hoc:
[ADR-0020](0020-tag-triggered-release-pipeline.md) introduced Homebrew and the
Ubuntu PPA together as an implementation detail of the release pipeline, and
this ADR adds Scoop for Windows.

Nothing in the record states *which* platforms are in scope, what a channel has
to provide before it can be added, or on what grounds one would be dropped. Two
narrower rules exist — [ADR-0021](0021-pin-runtime-binary-to-declared-dependency.md)
fixed how the Java runtime is resolved, and
[ADR-0023](0023-ubuntu-release-target-policy.md) fixed which Ubuntu series the
PPA matrix targets — but both were written as one-channel patches. The general
policy lived only in the shape of `.github/workflows/release.yml`.

Adding a third channel makes that gap expensive. Without a stated contract, each
new package manager invites its own launcher script, its own build, and its own
notion of where the package index lives; three divergent channels are more than
one maintainer can keep correct.

## Decision

jPipe supports **one package manager per mainstream desktop operating system**:

| OS | Channel | Index | Release asset | Secret |
|----|---------|-------|---------------|--------|
| macOS | Homebrew | `jpipe-mcscert/homebrew-mcscert` (tap) | `jpipe-X.Y.Z.tar.gz` | `HOMEBREW_TAP_TOKEN` |
| Ubuntu | APT / Launchpad PPA | `ppa:mcscert/ppa` | Debian source package (built from the fat JAR) | `GPG_*` |
| Windows | Scoop | `jpipe-mcscert/scoop-mcscert` (bucket) | `jpipe-X.Y.Z.zip` | `SCOOP_BUCKET_TOKEN` |

Every channel must satisfy the following contract:

1. **One launcher source.** The channel installs a launcher template from `bin/`
   — `bin/jpipe` for POSIX shells, `bin/jpipe.ps1` for PowerShell — using the
   `@@JAVA@@` / `@@PREFIX@@` placeholder convention. A channel does not carry
   its own launcher implementation.
2. **Dependencies are declared, and the runtime is resolved from them.** The
   channel declares Java 25 and Graphviz as package-manager dependencies, and
   substitutes `@@JAVA@@` with the exact binary owned by the declared
   dependency, resolved through the package manager's own registry at install
   time — never with a bare `java`. This generalises ADR-0021 from the two
   original channels to any channel.
3. **Channels consume, they do not build.** The channel installs a versioned
   asset published on the GitHub Release created by the tag-triggered pipeline.
   No channel compiles from source, so every platform ships bit-identical
   bytecode from a single `mvn verify`.
4. **The index is ours and machine-updated.** The formula, manifest, or PPA
   lives in a repository or service the project controls, and is updated by a
   push from `release.yml`. Package indices are never edited by hand, and
   jPipe is not published to third-party-curated indices that would take the
   update out of the pipeline's hands.
5. **Stable releases only.** Tags containing `-` (e.g. `v2.3.0-rc1`) publish a
   GitHub Release marked as a pre-release, but every channel job is skipped.
   Pre-releases are for verifying the pipeline, not for distribution.
6. **Superseded versions stay installable.** Each channel keeps older releases
   reachable through its own native mechanism — versioned formulas
   (`jpipe@X.Y.Z.rb`) for Homebrew, manifest git history
   (`scoop install jpipe@X.Y.Z`) for Scoop, and Launchpad's published version
   history for the PPA. The project does not maintain a separate archive.

Adding a channel means adding a release asset, a `release.yml` job, and a
credential — and meeting all six points. A channel is dropped when its platform
is no longer mainstream for jPipe's audience, or when its index can no longer be
updated automatically.

## Rationale

- **The contract is what makes three channels affordable.** A single launcher
  source, a single build, and a single trigger mean a release is one tag push
  regardless of how many platforms are served. Channel-specific launchers or
  builds would multiply the surface that can break between releases, and each
  would break on a platform the maintainer does not run day to day.
- **Point 2 generalises a rule that was learned the hard way.** ADR-0021 was
  written after the launcher was found to invoke whichever JVM happened to be
  the system default, defeating the declared dependency. Stating the rule as a
  channel-admission criterion prevents rediscovering it once per package
  manager — Scoop's `post_install` substitution exists because of this clause.
- **Point 3 removes a whole class of divergence.** If Scoop rebuilt from source
  it would need a JDK on Windows, a Maven cache, and a green Windows build —
  none of which CI has. Consuming the release asset means the Windows package
  contains the JAR that macOS and Ubuntu users already run.
- **Point 4 keeps release state out of human hands.** The failure mode for
  hand-edited package indices is a formula pointing at a URL whose checksum no
  longer matches. Making the pipeline the only writer makes that unrepresentable.
- **Point 6 lets users pin without the project hoarding.** Each package manager
  already solves version pinning; adopting the native mechanism per channel is
  cheaper and more idiomatic than a project-run artifact archive. It is also why
  the Scoop bucket needs no pinned per-version manifests: one commit per release
  against `bucket/jpipe.json` is exactly what Scoop's `@version` lookup walks.
- **One channel per OS, not the most popular one.** Chocolatey and WinGet are
  larger Windows ecosystems than Scoop, but both are curated: publishing means
  submitting a package for review, which violates point 4. Scoop's bucket model
  lets the project own the index outright, matching how the Homebrew tap and the
  PPA already work.

## Consequences

- Five GitHub Actions secrets are now required for a complete release:
  `HOMEBREW_TAP_TOKEN`, `SCOOP_BUCKET_TOKEN`, `GPG_PRIVATE_KEY`, `GPG_KEY_ID`,
  and `GPG_PASSPHRASE`. A missing credential fails one channel job, not the
  release itself.
- `bin/` is a channel-neutral launcher directory, not a Homebrew-specific one.
  It holds `jpipe` (POSIX `sh`) and `jpipe.ps1` (PowerShell); both keep a
  working fallback when their placeholders are unsubstituted, so they can be run
  directly from a source checkout.
- Windows is the only channel CI cannot smoke-test: the release workflow runs
  entirely on `ubuntu-latest` and no Windows runner exists. Verifying a Scoop
  release requires a Windows machine and remains a manual release-checklist step.
- `jpipe-mcscert/scoop-mcscert` must contain `bucket/jpipe.json` before the
  first stable tag after this ADR; `update-scoop` updates an existing manifest
  and does not create one.
- ADR-0023 is unaffected and remains the sub-policy governing *which* Ubuntu
  series the PPA matrix targets. This ADR governs the set of channels; ADR-0020
  owns the pipeline mechanics that implement them.
