# ADR-0020: Tag-Triggered Release Pipeline

**Date:** 2026-04-21
**Status:** Accepted

## Context

As jPipe matures, stable releases must be distributed to end-users through
standard package managers. Before this ADR, no automated release process
existed: the build produced a fat JAR, but no versioned artifacts and no
package-manager update.

Additionally, the version shown by `jpipe doctor` was hardcoded as a string
literal (`"jPipe 2.0.0"`) in the `@Command` annotation of `Main.java`. This
was a separate, manually maintained value that could silently diverge from
the project version in `pom.xml`.

Which platforms jPipe targets, and what a packaging channel must provide, is a
separate concern owned by
[ADR-0025](0025-mainstream-platform-distribution.md). This ADR owns the
*mechanics*: how a release is triggered, validated, built, and handed to each
channel.

## Decision

A workflow (`.github/workflows/release.yml`) is triggered when a tag matching
`v*.*.*` is pushed. The workflow:

1. **Verifies** that the tag points at a commit on `main`, which is release-only
   under [ADR-0024](0024-git-branching-model.md).
2. **Validates** that the tag version matches the base version declared in
   `pom.xml` (stripping any `-SNAPSHOT` suffix). The build fails fast if they
   diverge.
3. **Sets** the final release version via `mvn versions:set` so the fat JAR
   manifest carries the correct `Implementation-Version`.
4. **Publishes a GitHub Release** with three assets:
   - `jpipe-cli-$VERSION.jar` — the fat JAR;
   - `jpipe-$VERSION.tar.gz` — the Homebrew payload (`jpipe.jar` plus the
     `bin/jpipe` launcher, in a versioned directory);
   - `jpipe-$VERSION.zip` — the Scoop payload (`jpipe.jar` plus the
     `bin/jpipe.ps1` launcher, flat at the archive root).
5. **Updates** the `jpipe.rb` formula in `jpipe-mcscert/homebrew-mcscert` (URL,
   SHA256, and `openjdk` dependency version) and writes a versioned
   `jpipe@$VERSION.rb` alongside it.
6. **Updates** `bucket/jpipe.json` in `jpipe-mcscert/scoop-mcscert` (version,
   URL, hash). The manifest's `autoupdate` block is left untouched, and is what
   lets Scoop reconstruct an older `scoop install mcscert/jpipe@X.Y.Z`.
7. **Builds and uploads** a signed Debian source package to `ppa:mcscert/ppa`
   on Launchpad, once per targeted Ubuntu series (see
   [ADR-0023](0023-ubuntu-release-target-policy.md)).

Steps 5–7 run in parallel, each in its own job depending on the build. The
launcher templates they install live in `bin/` at the repository root,
symmetric with the `debian/` directory that Debian tooling requires there.

`pom.xml` becomes the **single source of truth** for the version. The fat JAR
manifest is populated with `Implementation-Version: ${project.version}` by
the Maven Shade plugin. `Main.java` reads this value at runtime via a
`CommandLine.IVersionProvider` implementation, with a `"dev"` fallback when
no manifest is present (IDE runs, unit tests).

## Rationale

- **Tag-based trigger keeps releases deliberate.** Merging to `dev` integrates a
  change; it does not ship one. A developer must explicitly merge `dev` into
  `main` and push a version tag for a release to happen.
- **Single source of truth eliminates drift.** The previous hardcoded string
  in `Main.java` was a maintenance hazard. Embedding the version from
  `pom.xml` via the manifest removes the need to update two places.
- **Fail-fast validation.** The workflow aborts immediately if the tag and
  `pom.xml` version disagree, or if the tag is not on `main`, preventing a
  release with a mismatched version from reaching package managers.
- **One build feeds every channel.** All assets derive from a single
  `mvn verify`, so every platform ships identical bytecode. Channel jobs
  consume release assets rather than rebuilding — a requirement of ADR-0025.

## Consequences

### Required secrets

Five GitHub Actions secrets must be configured in the repository:

| Secret | Purpose |
|--------|---------|
| `HOMEBREW_TAP_TOKEN` | PAT with `contents: write` on `jpipe-mcscert/homebrew-mcscert` |
| `SCOOP_BUCKET_TOKEN` | PAT with `contents: write` on `jpipe-mcscert/scoop-mcscert` |
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
3. Merge `dev` into `main` and push the tag — the pipeline fires automatically.
4. Merge `main` back into `dev`, then bump `pom.xml` to the next development
   version (`mvn -B versions:set -DnewVersion=X.Y+1.0-SNAPSHOT`) on a topic
   branch merged into `dev`.

Tags containing `-` (e.g. `v2.1.0-rc1`) are automatically marked as pre-releases;
all three channel jobs are skipped for them.

See the "Releasing a new version" section in `README.md` for the full
step-by-step procedure.

### Downstream effects

- `jpipe doctor` now reports the version embedded in the JAR manifest. When
  running from source (no fat JAR), it reports `jPipe dev`.
- The `templates/` directory is removed; `bin/` at the repository root holds the
  launcher templates for every packaging channel.
- The `debian/` directory at the repository root is required by
  `dpkg-buildpackage` and must not be moved.
