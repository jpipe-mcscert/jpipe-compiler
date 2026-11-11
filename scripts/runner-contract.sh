#!/usr/bin/env bash
#
# Contract test between jPipe's Python export and jpipe-runner (#165).
#
# For each fixture below, export the justification (-f JSON) and its step
# library (-f PYTHON) with the given fat JAR, then dry-run the library on the
# justification with the installed jpipe-runner. A freshly generated library
# must import, bind every step, and be refused only for what a skeleton cannot
# know: evidence that observes nothing (JP018) and produces nothing (JP012).
# Any other diagnostic means the generated code and the runner's API drifted
# apart. The same holds once the commented-out conclusion and sub-conclusions
# are uncommented, as the generated note invites.
#
# Usage: scripts/runner-contract.sh <jpipe-cli fat JAR>
#   JPIPE_RUNNER  the runner command (default: jpipe-runner on PATH)
#   PYTHON        a Python interpreter to read the JSON report (default: python3)

set -euo pipefail

JAR=${1:?usage: $0 <jpipe-cli fat JAR>}
RUNNER=${JPIPE_RUNNER:-jpipe-runner}
PYTHON=${PYTHON:-python3}
ROOT=$(cd "$(dirname "$0")/.." && pwd)

# <example file>:<justification> -- one per shape the exporter handles: a plain
# model, a template implementation, a merge bound by several ids, a refine whose
# hook becomes a sub-conclusion, and elements sharing a label, whose functions
# must keep distinct names for the runner to see both steps.
FIXTURES=(
  "000_minimal.jd:minimal"
  "005_override.jd:indirect"
  "011_unifying_while_compising.jd:assembled_2"
  "012_chaining_operators.jd:with_refine_1"
  "009_refine.jd:expected"
  "010_assemble.jd:expected_3"
)

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT

"$RUNNER" --version

# Reads a JSON dry-run report on stdin; fails unless the verdict is invalid
# and every diagnostic is a JP012 or JP018 error, both codes appearing.
check_report() {
  "$PYTHON" -I -c '
import json, sys
label = sys.argv[1]
report = json.load(sys.stdin)
found = [(d["code"], d["severity"]) for d in report["diagnostics"]]
codes = {code for code, _ in found}
verdict = report["verdict"]
unexpected = [d for d in found if d[0] not in ("JP012", "JP018") or d[1] != "error"]
if unexpected or codes != {"JP012", "JP018"} or verdict != "invalid":
    sys.exit(f"{label}: expected only JP012 and JP018 errors, got {found} (verdict {verdict})")
print(f"{label}: ok ({len(found)} diagnostics, all JP012/JP018)")
' "$1"
}

# Runs a dry run, keeping the JSON report; the runner exits non-zero on an
# invalid verdict, which is the expected outcome here.
dry_run() {
  "$RUNNER" --library "$1" --dry-run --json "$2" > "$3" || true
}

failed=0
for fixture in "${FIXTURES[@]}"; do
  file=${fixture%%:*}
  model=${fixture##*:}
  json="$WORK/$model.json"
  steps="$WORK/${model}_steps.py"
  live="$WORK/${model}_live_steps.py"

  java -jar "$JAR" process -i "$ROOT/examples/$file" -m "$model" -f JSON -o "$json" > /dev/null
  java -jar "$JAR" process -i "$ROOT/examples/$file" -m "$model" -f PYTHON -o "$steps" > /dev/null

  dry_run "$steps" "$json" "$WORK/$model.report.json"
  check_report "$model" < "$WORK/$model.report.json" || failed=1

  # Uncomment every commented-out step, leaving the note above it a comment.
  sed -E 's/^# (@|def |    )/\1/' "$steps" > "$live"
  dry_run "$live" "$json" "$WORK/$model.live.report.json"
  check_report "$model (uncommented)" < "$WORK/$model.live.report.json" || failed=1
done

exit "$failed"
