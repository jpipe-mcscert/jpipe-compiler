#!/usr/bin/env bash
#
# Fails when a version pinned in <dependencyManagement> lies outside a version
# range that a dependency declares for the same artifact (#207).
#
# Cucumber declares some of its dependencies as ranges; the root pom pins them
# so that Maven never has to resolve a range. Maven then applies the pin
# silently, even when a Cucumber update has moved its range past it, and the
# enforcer's requireUpperBoundDeps does not see it either. This reads the
# verbose dependency tree, where Maven reports every such override as
# "version managed from [range]", and checks each pin against each range.
#
# It fails closed: finding no range override at all means the tree no longer
# reports them (maven-dependency-plugin 3.7.0 leaves them out, hence the
# version pinned in the pom), not that every pin is fine. Once Cucumber stops
# declaring ranges and the pins are gone, remove this check with them.
#
# Usage: scripts/check-managed-ranges.sh   (from the repository root)

set -euo pipefail

tree=$(mktemp)
trap 'rm -f "$tree"' EXIT

mvn -B -q dependency:tree -Dverbose -DoutputFile="$tree" -DappendOutput=true

python3 -I - "$tree" <<'EOF'
import re
import sys

# "io.cucumber:messages:jar:34.2.1:test - version managed from [32.0.0,35.0.0)"
# "io.cucumber:query:jar:16.4.0:test (version managed from [15.0.0,17.0.0)"
OVERRIDE = re.compile(
    r"([\w.\-]+:[\w.\-]+):[\w\-]+:([\w.\-]+):\w+[ (\-]+version managed from "
    r"([\[(][^\])]*[\])])")


NUMERIC = re.compile(r"\d+(\.\d+)*")


def key(version):
    """
    Orders dotted numeric versions the way Maven does: missing trailing
    components count as zero, so 35.0 equals 35.0.0. Anything else (a
    qualifier such as -rc1) is out of scope, and fails the check rather than
    being ordered wrongly.
    """
    if not NUMERIC.fullmatch(version):
        raise ValueError(version)
    parts = [int(p) for p in version.split(".")]
    while len(parts) > 1 and parts[-1] == 0:
        parts.pop()
    return parts


def within(version, spec):
    low, high = (bound.strip() for bound in spec[1:-1].split(","))
    v = key(version)
    if low and (v < key(low) or (spec[0] == "(" and v == key(low))):
        return False
    if high and (v > key(high) or (spec[-1] == ")" and v == key(high))):
        return False
    return True


checked, failures = set(), []
with open(sys.argv[1], encoding="utf-8") as tree:
    for line in tree:
        for artifact, version, spec in OVERRIDE.findall(line):
            if "," not in spec:
                continue  # a single version, not a range
            checked.add((artifact, version, spec))
            try:
                inside = within(version, spec)
            except ValueError as unsupported:
                failures.append(f"{artifact}:{version} cannot be checked"
                                f" against {spec}: '{unsupported}' is not a"
                                " dotted numeric version")
                continue
            if not inside:
                failures.append(f"{artifact}:{version} is outside {spec}")

for failure in sorted(set(failures)):
    print(f"ERROR: {failure} (a range a dependency declares)", file=sys.stderr)
if failures:
    print("Move the pin in the root pom.xml to a version every range accepts"
          " (see the comment next to cucumber.version).", file=sys.stderr)
    sys.exit(1)
if not checked:
    print("ERROR: the dependency tree reports no managed version overriding a"
          " range, so nothing was checked. Check the maven-dependency-plugin"
          " version, or remove this check if no range is pinned any more.",
          file=sys.stderr)
    sys.exit(1)
print(f"{len(checked)} managed version(s) checked against the ranges they"
      " override: all within.")
EOF
