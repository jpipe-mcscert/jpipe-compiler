# Releasing a new version

This is the release checklist. Work through the phases in order; each one ends
with a single command or a single decision.

**The shape of a release.** Day-to-day work accumulates on `dev`. A release closes
out the `CHANGELOG.md` `[Unreleased]` section on a release branch, `release/X.Y.Z`,
cut from `dev`, merges that branch into `main` through one pull request, and pushes a
`vX.Y.Z` tag on `main`. The tag — nothing else —
triggers `release.yml`, which publishes the GitHub Release and updates Homebrew,
Scoop and the Ubuntu PPA. See [ADR-0024](adr/0024-git-branching-model.md) for the
branch model and [ADR-0020](adr/0020-tag-triggered-release-pipeline.md) for the
pipeline mechanics and the list of required repository secrets.

`scripts/release.sh` does the mechanical parts. It never merges, never tags, and
never pushes — those stay in your hands.

---

## Phase 0 — Decide

Three judgement calls, none of which a script can make for you.

**Pick the version number.** Read the `[Unreleased]` section and apply semantic
versioning ([ADR-0001](adr/0001-semver-versioning.md)) to the CLI's public
interface: the language, the commands and their options and exit codes, the
export formats, and the diagnostics. A change that can break a user or a tool
relying on it is a MAJOR bump, new behaviour is MINOR, fixes alone are PATCH.
The Java modules are internal and do not count. What `pom.xml` currently says is *not* an input to
this decision; Phase 1 sets the pom to whatever you choose here.

**Review held-back dependencies** ([ADR-0007](adr/0007-dependency-freshness-policy.md)) —
a required step, not optional housekeeping. Dependabot keeps `dev` current week
by week, in one pull request; what is left is what it holds back (the majors
ignored in `.github/dependabot.yml`) and that pull request, if it is still open.
Decide, for each, whether it stays held for another cycle:

```bash
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates
```

**Review the Ubuntu series matrix**
([ADR-0023](adr/0023-ubuntu-release-target-policy.md)). Compare the `distro:` list
in `.github/workflows/release.yml` against the policy: every LTS released in or
after 2024 that is still in standard support, plus the next LTS once its series
opens on Launchpad, plus every interim release inside its nine-month window. Add
a series when it enters support; drop one in the release that follows its EOL.

---

## Phase 1 — Prepare, on a release branch

```bash
git switch dev && git pull --ff-only
git switch -c release/X.Y.Z
scripts/release.sh prepare X.Y.Z
```

The release branch holds everything `dev` holds: the release is what `dev` holds
now. `prepare` refuses any other branch, and one that lacks commits of `origin/dev`.

This sets every pom to `X.Y.Z-SNAPSHOT`, renames `## [Unreleased]` to
`## [X.Y.Z] — <today>`, opens a fresh empty `[Unreleased]` above it, adds the
`[X.Y.Z]` compare link at the bottom of the changelog, runs `mvn verify`, and
commits. Add `--dry-run` to see the diff without writing anything.

Setting the pom here is what makes the version safe. The pipeline compares the
tag against the pom version and fails **after the tag has been pushed**, so the
pom must say `X.Y.Z-SNAPSHOT` before you tag `vX.Y.Z` — whatever `dev` happened
to be bumped to earlier is irrelevant once `prepare` has run.

Then push the branch:

```bash
git push -u origin release/X.Y.Z
```

---

## Phase 2 — Release pull request

```bash
scripts/release.sh preflight X.Y.Z
```

Preflight re-runs every check `release.yml` performs, while the tag still does
not exist: clean tree, branch in sync, pom version against the tag base, tag not
already taken, changelog closed out, `mvn verify` green. It also reports whether
`HEAD` is on `main` yet — expect a warning here, since the merge has not
happened.

Open a pull request from `release/X.Y.Z` to `main` titled `Release vX.Y.Z`, take it
through the organization's
[pull request steps](https://github.com/jpipe-mcscert/.github/blob/main/CONTRIBUTING.md#pull-requests)
(draft, quality gate, one Copilot review, every comment answered), and merge it
once it is ready. This is the one merge in the process that must go
through a pull request: `main` is release-only, and every commit on it is a
published version. Its diff is the whole release: what `dev` merged since the
last one, and the release commit. The release commit goes into `main` without a
pull request into `dev` first; Phase 5 brings it to `dev`
([ADR-0024, amended](adr/0024-git-branching-model.md#amendment-2026-11-21-a-release-is-a-release-branch-into-main)).
Work merged into `dev` meanwhile waits for the next release.

---

## Phase 3 — Tag, on `main`

```bash
git switch main && git pull --ff-only
scripts/release.sh preflight X.Y.Z    # now expect "HEAD is on main"
git tag vX.Y.Z
git push origin vX.Y.Z
```

Pushing the tag is what starts the release. `release.yml` verifies the tag is an
ancestor of `main`, re-checks it against `pom.xml`, stamps the version into the
fat JAR manifest, builds once, and publishes the GitHub Release with three
assets. The Homebrew, Scoop and PPA jobs then run in parallel off that release.

A tag containing a hyphen (`v2.4.0-rc1`) is published as a GitHub pre-release and
**every** channel job is skipped
([ADR-0025](adr/0025-mainstream-platform-distribution.md)). A release candidate is
cut from an already-prepared version — do not run `prepare` for it.

Watch the run: <https://github.com/jpipe-mcscert/jpipe-compiler/actions>

---

## Phase 4 — Verify

Package indices take up to about thirty minutes to settle.

```bash
# macOS
brew tap jpipe-mcscert/mcscert
brew install jpipe && jpipe doctor

# Ubuntu
sudo add-apt-repository ppa:mcscert/ppa
sudo apt update && sudo apt install jpipe && jpipe doctor
```

```powershell
# Windows
scoop bucket add mcscert https://github.com/jpipe-mcscert/scoop-mcscert
scoop install mcscert/jpipe
jpipe doctor
```

`jpipe doctor` should report the version you just tagged. **The Windows check is
manual and required** — it is the one channel CI cannot smoke-test
([ADR-0025](adr/0025-mainstream-platform-distribution.md)).

Each install should also have created the AOT cache next to `jpipe.jar`. No CI
job runs a channel's install step, so check it on each platform you install on:

```bash
ls "$(brew --prefix jpipe)/jpipe.aot"      # macOS
ls /usr/share/jpipe/jpipe.aot              # Ubuntu
```

```powershell
Test-Path "$(scoop prefix jpipe)\jpipe.aot"   # Windows
```

A missing cache does not break jPipe, only slows its start-up; run
`jpipe --create-aot-cache` by hand to see why it was not created.

---

## Phase 5 — Merge back and reopen development

```bash
git switch dev && git pull --ff-only
git merge origin/main
scripts/release.sh post-release <next>-SNAPSHOT   # e.g. 2.4.1-SNAPSHOT
git push
```

Pass the next version literally — `<next>` is a placeholder, and the script
rejects anything that is not a valid Maven version. The next **patch** is the
low-surprise default: since Phase 1 sets the release version explicitly, an
over-bump here is harmless, but it is what confused the 2.3.1 release.

Merging `main` back into `dev` brings the release commit to `dev`, so that the
two branches share it; `post-release` refuses to run until you have. Delete the
release branch once it is merged.

---

## Troubleshooting

**The tag is pushed and the pipeline failed on the version check.** The tag is
not on the release yet, so it is safe to withdraw:

```bash
git push --delete origin vX.Y.Z
git tag -d vX.Y.Z
```

Fix the pom on a new release branch (`scripts/release.sh prepare X.Y.Z`), take it
into `main` again, and re-tag. Running `preflight` in Phase 3 is what prevents this.

**The release published but a channel job failed.** By design, a missing
credential or a broken index fails one channel, not the release — the GitHub
Release and its assets are already live. Fix the cause and re-run the failed job
from the Actions run page; the channel jobs consume release assets rather than
rebuilding, so re-running is safe and produces identical bytecode.

**`prepare` says `[Unreleased]` is empty.** Nothing has been merged into `dev`
since the last release, or the entries were never written. The changelog is the
release notes — write them before releasing, not after.

**`post-release` says main is not merged back.** Run `git merge origin/main` on
`dev` first. Skipping this leaves the release commit on `main` only, and the next
release branch's merge into `main` becomes a conflict.
