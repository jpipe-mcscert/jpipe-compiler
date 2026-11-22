# scoop-mcscert

[![Tests](https://github.com/jpipe-mcscert/scoop-mcscert/actions/workflows/ci.yml/badge.svg)](https://github.com/jpipe-mcscert/scoop-mcscert/actions/workflows/ci.yml)

A [Scoop](https://scoop.sh) bucket for software released by
[McSCert](https://www.mcscert.ca/) (McMaster Centre for Software Certification).

## Installation

```pwsh
scoop install git
scoop bucket add java
scoop bucket add mcscert https://github.com/jpipe-mcscert/scoop-mcscert
scoop install mcscert/jpipe
```

All four steps are required:

- **`git`** — `scoop bucket add` clones the bucket repository, and Scoop
  refuses to add a bucket without it.
- **the `java` bucket** — jPipe depends on `java/temurin25-jre`. Scoop
  resolves dependencies only against buckets you have already added; it will
  **not** add a missing one for you, and the install fails if `java` is
  absent. Graphviz comes from `main`, which Scoop adds by default, so it
  needs no equivalent step.

## Manifests

| App | Description | Source |
|-----|-------------|--------|
| `jpipe` | Compiler and language environment for justification models | [jpipe-mcscert/jpipe-compiler](https://github.com/jpipe-mcscert/jpipe-compiler) |

### Installing a specific version

The bucket manifest always names the current release, but an older one can be
requested explicitly:

```pwsh
scoop install mcscert/jpipe@2.3.0    # install a specific release
scoop reset jpipe@2.4.0              # switch to an already-installed version
```

Scoop 0.6.0 and later find the older manifest in this bucket's **git
history**: each release is one commit, which holds that release's URL, the hash
its pipeline recorded, and its own `depends` and `post_install`. Older Scoop
versions, or Scoop with `USE_GIT_HISTORY` turned off, fall back to the
`autoupdate` block instead: they substitute the requested version into the
download URL, hash whatever they download, and apply today's `depends` and
`post_install` to the old version.

**Versions before 2.3.0 are not available through Scoop.** They predate
Windows packaging and publish no `.zip` asset. `@2.2.0` resolves to the
hand-written seed manifest, whose download fails. Take those versions from the
[jpipe-compiler releases page](https://github.com/jpipe-mcscert/jpipe-compiler/releases)
instead.

### Faster start-up

Installing jPipe runs `jpipe --create-aot-cache`, which records a cache that
makes every call start faster. It only works for the Java build that created
it, so after `scoop update temurin25-jre`, run `jpipe --create-aot-cache` again.
Without a valid cache, jPipe runs as before, only slower to start.

## Maintenance

**This repository is written only by the release pipelines of the projects it
distributes, and must not be edited by hand.** For jPipe, the `update-scoop`
job of `release.yml` in jpipe-compiler writes `bucket/jpipe.json` and this
README in full from the templates in its
[`packaging/scoop/`](https://github.com/jpipe-mcscert/jpipe-compiler/tree/main/packaging/scoop)
directory, filling in the `version`, `url`, and `hash` of the release. A change
to the manifest or to this README is made there, and reaches this repository
with the next release.

**The history of `main` must never be rewritten.** Each release commit is the
manifest Scoop 0.6.0 and later install for that version: a force-push, a squash
or a rebase would remove every older version from those users.

Manifests carry `autoupdate` but deliberately **no `checkver`**: the block is
used only to reconstruct a manifest when someone asks for an explicit
`@version`. Nothing in this repository polls for new releases or updates
itself — the source repository's release pipeline remains the only writer.

For jPipe, that policy is recorded in
[ADR-0025](https://github.com/jpipe-mcscert/jpipe-compiler/blob/main/docs/adr/0025-mainstream-platform-distribution.md).

## Reporting problems

Issues with a packaged application belong in that application's own repository.
Use this repository's issue tracker only for problems with the manifests
themselves — a bad hash, a missing dependency, or a broken shim.
