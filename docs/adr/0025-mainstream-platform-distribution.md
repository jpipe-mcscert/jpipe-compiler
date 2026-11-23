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
4. **The index is ours and machine-written.** The formula, manifest, or PPA
   lives in a repository or service the project controls, and only
   `release.yml` writes to it. The Homebrew formula and the Scoop manifest are
   templates in this repository, under `packaging/homebrew/` and
   `packaging/scoop/` (with the bucket's README), which the release renders and
   writes to the tap and the bucket in full. Package indices are never edited
   by hand, and jPipe is not published to third-party-curated indices that
   would take the update out of the pipeline's hands.
5. **Stable releases only.** Tags containing `-` (e.g. `v2.3.0-rc1`) publish a
   GitHub Release marked as a pre-release, but every channel job is skipped.
   Pre-releases are for verifying the pipeline, not for distribution.
6. **Superseded versions stay installable.** Each channel keeps older releases
   reachable through its own native mechanism — versioned formulas
   (`jpipe@X.Y.Z.rb`) for Homebrew; for Scoop, the bucket's git history, which
   holds one commit per release, with an `autoupdate` block as the fallback;
   and Launchpad's published version history for the PPA. The project does not
   maintain a separate archive. A channel reaches only as far back as the first
   release that published its asset.

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
  Keeping the whole formula and manifest here, rather than patching three fields
  of a file the index owns, also means a change to how a channel installs jPipe
  (a new dependency, a `post_install` step) is reviewed and released with the
  code that needs it.
- **Point 6 lets users pin without the project hoarding.** Each package manager
  already solves version pinning; adopting the native mechanism per channel is
  cheaper and more idiomatic than a project-run artifact archive. Scoop 0.6.0
  and later answer `scoop install mcscert/jpipe@X.Y.Z` by searching the
  bucket's git history for the commit that held that version. Each release
  writes exactly one commit, so that history is the archive, and each version
  comes back with the hash its own pipeline recorded and its own `depends` and
  `post_install`. Older Scoop, or Scoop with `USE_GIT_HISTORY` turned off, falls
  back to the `autoupdate` block: `generate_user_manifest` substitutes the
  requested version into the release URL, hashes whatever it downloads, and
  applies today's `depends` and `post_install` to the old version. A single
  templatable URL therefore covers every version, and the bucket stays one file
  per app: separate per-version manifests are not idiomatic for Scoop, since
  `@` cannot be part of an app name and each extra app would put its own
  `jpipe` shim on the `PATH`. The block carries no `checkver`, so nothing polls
  or self-updates and point 4 still holds: the release pipeline remains the
  only writer.
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
  It holds `jpipe` (POSIX `sh`) and `jpipe.ps1` (PowerShell). The two differ in
  how far they get without substitution: `jpipe.ps1` falls back to `PATH` and
  its own directory, so it runs unchanged from a source checkout, whereas
  `jpipe` invokes `@@JAVA@@` directly and only keeps its pre-flight `which java`
  check intact (ADR-0021) — it requires substitution to run. Aligning the POSIX
  launcher would mean rewriting the line that `debian/postinst` rewrites by
  exact string match, so it is deliberately left alone here.
- Windows is the only channel CI cannot smoke-test: the release workflow runs
  entirely on `ubuntu-latest` and no Windows runner exists. Verifying a Scoop
  release requires a Windows machine and remains a manual release-checklist step.
- **The bucket's history must never be rewritten.** A force-push, squash or
  rebase on `main` of `jpipe-mcscert/scoop-mcscert` would remove every older
  version for Scoop 0.6.0 users. The bucket's README says so, and its `main`
  branch is protected against force-pushes.
- Versions before 2.3.0 are not available through Scoop: they publish no `.zip`
  asset, and `@2.2.0` resolves to the hand-written seed manifest, whose
  download fails. The bucket's README documents it; fixing it would mean
  rewriting the history.
- Each channel runs `jpipe --create-aot-cache` once after installing (Debian's
  `postinst`, Homebrew's `post_install_steps`, Scoop's `post_install`), which records
  an AOT cache next to `jpipe.jar` from the training model `aot-training.jd`
  shipped with it (#216). The cache only works for the Java build and the JAR
  path that created it, so it cannot ship in the release assets. After a Java
  upgrade it is stale: the launchers ignore it silently, and running
  `jpipe --create-aot-cache` again (or `brew postinstall jpipe`) restores it.
  Debian's `prerm` removes it, since dpkg does not track it.
- ADR-0023 is unaffected and remains the sub-policy governing *which* Ubuntu
  series the PPA matrix targets. This ADR governs the set of channels; ADR-0020
  owns the pipeline mechanics that implement them.
