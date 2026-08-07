#!/usr/bin/env bash
#
# release.sh — drive a jPipe release from the command line.
#
# Three verbs, matching the phases in docs/releasing.md:
#
#   preflight X.Y.Z          re-run release.yml's checks locally, before the tag exists
#   prepare   X.Y.Z          set the pom version and close out the changelog, on dev
#   post-release X.Y.Z-SNAPSHOT   bump to the next development version, on dev
#
# The script never merges a branch, never creates or pushes a tag, and never
# pushes anything. Those stay human actions (see CLAUDE.md).

set -euo pipefail

MVN=${MVN:-mvn}
REPO_URL_FALLBACK="https://github.com/jpipe-mcscert/jpipe-compiler"

DRY_RUN=0
FAILURES=0

# ----------------------------------------------------------------------------
# output helpers
# ----------------------------------------------------------------------------

if [ -t 1 ]; then
  C_RED=$'\033[31m'; C_GREEN=$'\033[32m'; C_YELLOW=$'\033[33m'
  C_BOLD=$'\033[1m'; C_OFF=$'\033[0m'
else
  C_RED=''; C_GREEN=''; C_YELLOW=''; C_BOLD=''; C_OFF=''
fi

pass() { printf '  %sok%s   %s\n' "$C_GREEN" "$C_OFF" "$1"; }
warn() { printf '  %swarn%s %s\n' "$C_YELLOW" "$C_OFF" "$1"; }
fail() { printf '  %sFAIL%s %s\n' "$C_RED" "$C_OFF" "$1"; FAILURES=$((FAILURES + 1)); }
head2() { printf '\n%s%s%s\n' "$C_BOLD" "$1" "$C_OFF"; }
die() { printf '%serror:%s %s\n' "$C_RED" "$C_OFF" "$1" >&2; exit 1; }
note() { printf '       %s\n' "$1"; }

usage() {
  cat <<'EOF'
Usage: scripts/release.sh <verb> [options]

Verbs:
  preflight X.Y.Z            Check that everything release.yml validates would pass,
                             before the tag exists. Read-only. Runs `mvn verify`.
  prepare X.Y.Z              On dev: set the pom to X.Y.Z-SNAPSHOT, close out the
                             CHANGELOG [Unreleased] section, verify, and commit.
  post-release X.Y.Z-SNAPSHOT
                             On dev, after main has been merged back: set the next
                             development version and commit.

Options:
  --dry-run                  Show what would change without writing anything
                             (prepare and post-release only).
  --skip-verify              Skip `mvn verify` (preflight and prepare).
  -h, --help                 This message.

The full runbook is in docs/releasing.md.
EOF
}

# ----------------------------------------------------------------------------
# small utilities
# ----------------------------------------------------------------------------

# A release version: X.Y.Z, optionally with a pre-release suffix (2.4.0-rc1).
valid_version() {
  printf '%s' "$1" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.]+)?$'
}

pom_version() {
  "$MVN" -B -q help:evaluate -Dexpression=project.version -DforceStdout 2>/dev/null | tail -1
}

current_branch() {
  git rev-parse --abbrev-ref HEAD
}

# Content of the CHANGELOG [Unreleased] section, blank lines stripped.
unreleased_body() {
  awk '
    /^## \[Unreleased\]/ { inside = 1; next }
    inside && /^---[[:space:]]*$/ { exit }
    inside && NF { print }
  ' CHANGELOG.md
}

run() {
  if [ "$DRY_RUN" -eq 1 ]; then
    printf '  %s[dry-run]%s %s\n' "$C_YELLOW" "$C_OFF" "$*"
  else
    "$@"
  fi
}

# ----------------------------------------------------------------------------
# individual checks
# ----------------------------------------------------------------------------

check_clean_tree() {
  if [ -n "$(git status --porcelain)" ]; then
    fail "working tree is dirty — commit or stash first"
    git status --short | sed 's/^/       /'
  else
    pass "working tree is clean"
  fi
}

check_branch_is() {
  local expected=$1 actual
  actual=$(current_branch)
  if [ "$actual" = "$expected" ]; then
    pass "on branch $expected"
  else
    fail "on branch $actual, expected $expected"
  fi
}

check_in_sync() {
  local branch upstream
  branch=$(current_branch)
  if ! upstream=$(git rev-parse --abbrev-ref "@{upstream}" 2>/dev/null); then
    warn "$branch has no upstream — cannot check whether it is current"
    return
  fi
  git fetch --quiet origin
  if [ "$(git rev-parse HEAD)" = "$(git rev-parse "$upstream")" ]; then
    pass "$branch is in sync with $upstream"
  elif git merge-base --is-ancestor "$upstream" HEAD; then
    warn "$branch is ahead of $upstream — remember to push"
  else
    fail "$branch is behind or diverged from $upstream — pull first"
  fi
}

# release.yml:22-28 — the tag must point at a commit on main.
check_on_main() {
  git fetch --quiet origin main
  if git merge-base --is-ancestor HEAD origin/main; then
    pass "HEAD is on main — release.yml's branch check will pass"
  else
    warn "HEAD is not on main yet — merge the dev → main release PR before tagging"
    note "release.yml refuses a tag that is not an ancestor of main"
  fi
}

# release.yml:50-58 — the tag base must equal the pom version with -SNAPSHOT stripped.
check_version_matches_pom() {
  local version=$1 pom base
  pom=$(pom_version)
  pom=${pom%-SNAPSHOT}
  base=${version%%-*}
  if [ "$base" = "$pom" ]; then
    pass "pom version ($pom) matches tag base ($base)"
  else
    fail "pom version is $pom but tag v$version has base $base"
    note "release.yml performs this exact comparison, and fails after the tag is pushed"
    note "fix with: scripts/release.sh prepare $base"
  fi
}

check_tag_absent() {
  local version=$1
  if git rev-parse -q --verify "refs/tags/v$version" >/dev/null; then
    fail "tag v$version already exists locally"
  elif [ -n "$(git ls-remote --tags origin "refs/tags/v$version" 2>/dev/null)" ]; then
    fail "tag v$version already exists on origin"
  else
    pass "tag v$version does not exist yet"
  fi
}

check_changelog_closed() {
  local version=$1
  if grep -q "^## \[$version\] — " CHANGELOG.md; then
    pass "CHANGELOG has a [$version] section"
  else
    fail "CHANGELOG has no '## [$version] — <date>' section"
    note "run: scripts/release.sh prepare $version"
  fi

  if grep -q "^\[$version\]: " CHANGELOG.md; then
    pass "CHANGELOG has a [$version] compare link"
  else
    fail "CHANGELOG has no '[$version]:' compare link at the bottom"
  fi

  if [ -z "$(unreleased_body)" ]; then
    pass "CHANGELOG [Unreleased] is empty"
  else
    fail "CHANGELOG [Unreleased] still has entries — they would ship unlisted"
  fi
}

check_build() {
  local log
  if [ "${SKIP_VERIFY:-0}" -eq 1 ]; then
    warn "skipping mvn verify (--skip-verify)"
    return
  fi
  log=$(mktemp -t jpipe-release-verify)
  printf '  ..   running mvn verify (this takes a while)\n'
  if "$MVN" -B verify >"$log" 2>&1; then
    pass "mvn verify is green"
    rm -f "$log"
  else
    fail "mvn verify failed — full log in $log"
    tail -20 "$log" | sed 's/^/       /'
  fi
}

print_manual_checklist() {
  head2 "Manual checks — these are required, not optional housekeeping"
  cat <<'EOF'
  [ ] Dependency review: `mvn versions:display-dependency-updates` (ADR-0007)
  [ ] Ubuntu series matrix in .github/workflows/release.yml still matches the
      policy in ADR-0023 — add a series entering support, drop one past EOL
  [ ] Windows/Scoop smoke test — CI cannot run it (ADR-0025)
  [ ] The version number matches what [Unreleased] actually contains (ADR-0001)
EOF
}

# ----------------------------------------------------------------------------
# verbs
# ----------------------------------------------------------------------------

cmd_preflight() {
  local version=$1
  head2 "Preflight for v$version"
  check_clean_tree
  check_in_sync
  check_on_main
  check_version_matches_pom "$version"
  check_tag_absent "$version"
  check_changelog_closed "${version%%-*}"
  check_build
  print_manual_checklist

  if [ "$FAILURES" -gt 0 ]; then
    printf '\n%s%d check(s) failed.%s Fix them before tagging.\n' "$C_RED" "$FAILURES" "$C_OFF"
    exit 1
  fi
  printf '\n%sReady.%s Next: merge the dev → main PR, then tag on main:\n' "$C_GREEN" "$C_OFF"
  printf '  git switch main && git pull --ff-only\n'
  printf '  git tag v%s && git push origin v%s\n' "$version" "$version"
}

rewrite_changelog() {
  local version=$1 today=$2 tmp link base prev
  today=${today:-$(date +%Y-%m-%d)}

  link=$(grep -m1 '^\[Unreleased\]: ' CHANGELOG.md || true)
  if [ -n "$link" ]; then
    base=${link#*: }
    base=${base%/compare/*}
    prev=${link##*/compare/}
    prev=${prev%%...*}
  else
    base=$REPO_URL_FALLBACK
    prev=""
  fi

  tmp=$(mktemp)
  awk -v ver="$version" -v today="$today" '
    !swapped && /^## \[Unreleased\]$/ {
      print "## [Unreleased]"
      print ""
      print "---"
      print ""
      print "## [" ver "] — " today
      swapped = 1
      next
    }
    { print }
  ' CHANGELOG.md >"$tmp"

  if [ -n "$prev" ]; then
    awk -v ver="$version" -v base="$base" -v prev="$prev" '
      /^\[Unreleased\]: / {
        print "[Unreleased]: " base "/compare/v" ver "...HEAD"
        print "[" ver "]: " base "/compare/" prev "...v" ver
        next
      }
      { print }
    ' "$tmp" >"$tmp.2" && mv "$tmp.2" "$tmp"
  fi

  if [ "$DRY_RUN" -eq 1 ]; then
    printf '  %s[dry-run]%s CHANGELOG.md would change:\n' "$C_YELLOW" "$C_OFF"
    diff -u CHANGELOG.md "$tmp" | sed 's/^/       /' || true
    rm -f "$tmp"
  else
    mv "$tmp" CHANGELOG.md
    pass "CHANGELOG closed out as [$version] — $today"
  fi
}

cmd_prepare() {
  local version=$1 today
  today=$(date +%Y-%m-%d)

  case $version in
    *-*) die "prepare takes a plain X.Y.Z; a pre-release tag (v$version) is cut from an already-prepared version" ;;
  esac

  head2 "Preparing the $version release"
  check_clean_tree
  check_branch_is dev
  check_in_sync
  check_tag_absent "$version"

  if [ -z "$(unreleased_body)" ]; then
    fail "CHANGELOG [Unreleased] is empty — there is nothing to release"
  else
    pass "CHANGELOG [Unreleased] has entries"
  fi

  if [ "$FAILURES" -gt 0 ]; then
    printf '\n%s%d check(s) failed.%s Nothing was changed.\n' "$C_RED" "$FAILURES" "$C_OFF"
    exit 1
  fi

  head2 "Applying"
  # The root pom is also the parent of every module, so a plain versions:set
  # rewrites all six poms; -DprocessAllModules is not needed here.
  run "$MVN" -B -q versions:set "-DnewVersion=$version-SNAPSHOT" -DgenerateBackupPoms=false
  [ "$DRY_RUN" -eq 1 ] || pass "pom version set to $version-SNAPSHOT"
  rewrite_changelog "$version" "$today"

  if [ "$DRY_RUN" -eq 1 ]; then
    printf '\n%sDry run — nothing was written.%s\n' "$C_YELLOW" "$C_OFF"
    return
  fi

  check_build
  if [ "$FAILURES" -gt 0 ]; then
    printf '\n%sBuild failed.%s The working tree holds the prepared changes; fix and commit yourself.\n' \
      "$C_RED" "$C_OFF"
    exit 1
  fi

  git add CHANGELOG.md pom.xml ./*/pom.xml
  git commit -q -m "chore: prepare the $version release"
  pass "committed: chore: prepare the $version release"

  print_manual_checklist
  printf '\n%sPrepared.%s Next:\n' "$C_GREEN" "$C_OFF"
  printf '  git push                                  # straight to dev, no PR needed\n'
  printf '  scripts/release.sh preflight %s\n' "$version"
  printf '  then open the dev → main release PR\n'
}

cmd_post_release() {
  local next=$1
  case $next in
    *-SNAPSHOT) : ;;
    *) die "post-release takes the next development version, e.g. 2.4.1-SNAPSHOT" ;;
  esac
  valid_version "$next" || die "not a valid version: $next"

  head2 "Bumping to $next"
  check_clean_tree
  check_branch_is dev
  check_in_sync

  git fetch --quiet origin main
  if git merge-base --is-ancestor origin/main HEAD; then
    pass "main has been merged back into dev"
  else
    fail "main is not merged back into dev yet"
    note "run: git merge origin/main"
  fi

  if [ "$FAILURES" -gt 0 ]; then
    printf '\n%s%d check(s) failed.%s Nothing was changed.\n' "$C_RED" "$FAILURES" "$C_OFF"
    exit 1
  fi

  head2 "Applying"
  run "$MVN" -B -q versions:set "-DnewVersion=$next" -DgenerateBackupPoms=false
  if [ "$DRY_RUN" -eq 1 ]; then
    printf '\n%sDry run — nothing was written.%s\n' "$C_YELLOW" "$C_OFF"
    return
  fi
  pass "pom version set to $next"

  git add pom.xml ./*/pom.xml
  git commit -q -m "chore: bump to $next"
  pass "committed: chore: bump to $next"
  printf '\n%sDone.%s Push straight to dev — no PR needed for a version bump.\n' "$C_GREEN" "$C_OFF"
}

# ----------------------------------------------------------------------------
# entry point
# ----------------------------------------------------------------------------

main() {
  local verb="" version="" arg
  SKIP_VERIFY=0

  for arg in "$@"; do
    case $arg in
      --dry-run) DRY_RUN=1 ;;
      --skip-verify) SKIP_VERIFY=1 ;;
      -h|--help) usage; exit 0 ;;
      -*) die "unknown option: $arg" ;;
      *)
        if [ -z "$verb" ]; then verb=$arg
        elif [ -z "$version" ]; then version=$arg
        else die "unexpected argument: $arg"
        fi
        ;;
    esac
  done

  [ -n "$verb" ] || { usage; exit 1; }

  cd "$(git rev-parse --show-toplevel)" ||
    die "not inside a git repository"
  command -v "$MVN" >/dev/null || die "maven not found on PATH"

  version=${version#v}
  case $verb in
    preflight|prepare)
      [ -n "$version" ] || die "$verb needs a version, e.g. scripts/release.sh $verb 2.4.0"
      valid_version "$version" || die "not a valid version: $version"
      ;;
    post-release)
      [ -n "$version" ] || die "post-release needs the next version, e.g. 2.4.1-SNAPSHOT"
      ;;
  esac

  case $verb in
    preflight) cmd_preflight "$version" ;;
    prepare) cmd_prepare "$version" ;;
    post-release) cmd_post_release "$version" ;;
    *) die "unknown verb: $verb (expected preflight, prepare or post-release)" ;;
  esac
}

main "$@"
