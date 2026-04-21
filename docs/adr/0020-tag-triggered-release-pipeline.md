# ADR-0020: Tag-Triggered Release Pipeline

**Date:** 2026-04-21
**Status:** Accepted

## Context

As jPipe matures, stable releases must be distributed to end-users through
standard package managers. Two primary channels were identified:

- **Homebrew** (macOS) — via the `jpipe-mcscert/homebrew-mcscert` tap
- **Ubuntu PPA** (Linux) — via `ppa:mcscert/ppa` on Launchpad

Before this ADR, no automated release process existed. The `unstable`
pre-release job in `build.yml` provided a rolling snapshot for CI consumers,
but it produced no versioned artifacts and did not update any package manager
formula.

Additionally, the version shown by `jpipe doctor` was hardcoded as a string
literal (`"jPipe 2.0.0"`) in the `@Command` annotation of `Main.java`. This
was a separate, manually maintained value that could silently diverge from
the project version in `pom.xml`.

## Decision

A new workflow (`.github/workflows/release.yml`) is triggered when a tag
matching `v*.*.*` is pushed to the repository. The workflow:

1. **Validates** that the tag version matches the base version declared in
   `pom.xml` (stripping any `-SNAPSHOT` suffix). The build fails fast if they
   diverge.
2. **Sets** the final release version via `mvn versions:set` so the fat JAR
   manifest carries the correct `Implementation-Version`.
3. **Publishes a GitHub Release** with the fat JAR and a Homebrew-compatible
   tarball (`jpipe-$VERSION.tar.gz` containing `jpipe.jar` + the `homebrew/jpipe`
   launcher script).
4. **Updates** the `jpipe.rb` formula in `jpipe-mcscert/homebrew-mcscert`
   (URL, SHA256, and `openjdk` dependency version).
5. **Builds and uploads** a signed Debian source package to `ppa:mcscert/ppa`
   on Launchpad.

The launcher script previously located at `templates/homebrew/jpipe` is moved
to `homebrew/jpipe` at the repository root, symmetric with the `debian/`
directory that Debian tooling requires at the root.

`pom.xml` becomes the **single source of truth** for the version. The fat JAR
manifest is populated with `Implementation-Version: ${project.version}` by
the Maven Shade plugin. `Main.java` reads this value at runtime via a
`CommandLine.IVersionProvider` implementation, with a `"dev"` fallback when
no manifest is present (IDE runs, unit tests).

## Rationale

- **Tag-based trigger keeps releases deliberate.** Every merge to `main` does
  not produce a stable release; a developer must explicitly create and push a
  version tag. The existing `unstable` pre-release on every `main` push
  continues to serve CI consumers.
- **Single source of truth eliminates drift.** The previous hardcoded string
  in `Main.java` was a maintenance hazard. Embedding the version from
  `pom.xml` via the manifest removes the need to update two places.
- **Fail-fast validation.** The workflow aborts immediately if the tag and
  `pom.xml` version disagree, preventing a release with a mismatched version
  from reaching package managers.
- **Homebrew and PPA are the supported install paths.** These two channels
  cover macOS and Ubuntu users respectively, matching the project's primary
  audience.

## Consequences

### Required secrets

Four GitHub Actions secrets must be configured in the repository:

| Secret | Purpose |
|--------|---------|
| `HOMEBREW_TAP_TOKEN` | PAT with `contents: write` on `jpipe-mcscert/homebrew-mcscert` |
| `GPG_PRIVATE_KEY` | ASCII-armored GPG private key registered on Launchpad |
| `GPG_KEY_ID` | Fingerprint of the signing key |
| `GPG_PASSPHRASE` | Passphrase for the signing key |

### Release procedure

The pipeline runs `mvn versions:set` internally, so the developer does **not**
need to remove the `-SNAPSHOT` suffix before tagging. The validation step strips
`-SNAPSHOT` from the pom version before comparing, so a pom at `2.1.0-SNAPSHOT`
is correct when releasing `v2.1.0`.

Manual steps required:

1. Verify the base version in `pom.xml` matches the intended tag
   (e.g. `2.1.0-SNAPSHOT` to release `v2.1.0`).
2. Run `mvn verify` locally to confirm the build is green.
3. Push the tag — the pipeline fires automatically.
4. After the pipeline completes, bump `pom.xml` to the next development version
   (`mvn -B versions:set -DnewVersion=X.Y+1.0-SNAPSHOT`) and push to `main`.

Tags containing `-` (e.g. `v2.1.0-rc1`) are automatically marked as pre-releases;
the Homebrew and PPA jobs are skipped for pre-releases.

See the "Releasing a new version" section in `README.md` for the full
step-by-step procedure.

### Downstream effects

- `jpipe doctor` now reports the version embedded in the JAR manifest. When
  running from source (no fat JAR), it reports `jPipe dev`.
- The `templates/` directory is removed; `homebrew/` at the repository root
  replaces it.
- The `debian/` directory at the repository root is required by
  `dpkg-buildpackage` and must not be moved.
