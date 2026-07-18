# ADR-0023: Ubuntu Release Target Policy

**Date:** 2026-07-18
**Status:** Accepted

## Context

[ADR-0020](0020-tag-triggered-release-pipeline.md) established that stable
releases are published to `ppa:mcscert/ppa` on Launchpad. The `publish-ppa`
job in `.github/workflows/release.yml` fans out over a matrix of Ubuntu series
(by codename), building one signed Debian source package per series.

ADR-0020 deliberately said nothing about *which* series that matrix should
contain. As a result, the set of targeted releases was an implicit decision
baked into a single line of YAML:

```yaml
distro: [jammy, noble, questing, resolute]
```

This is a moving target. Ubuntu ships a new interim release every six months
and a new LTS every two years; interim releases receive only **9 months** of
support, after which their PPA builds become dead weight (packages nobody can
`apt` an update for, and a build leg that only consumes CI minutes). Without a
written rule, a maintainer editing this line has no principled basis for
deciding which codenames to add or drop, and the matrix drifts — either
accumulating end-of-life series or dropping still-supported ones by accident.

This is the packaging-distribution analogue of the Java-runtime rule in
[ADR-0007](0007-dependency-freshness-policy.md): target long-lived releases,
don't chase or cling to short-lived ones.

## Decision

The `publish-ppa` matrix targets exactly:

1. **Every Ubuntu LTS released in or after 2024**, for as long as it remains in
   standard support, plus the next LTS once its development series opens.
2. **Every interim (non-LTS) release currently inside its 9-month support
   window.**

A series is **added** when it enters standard support (or, for the next LTS,
when its development series opens on Launchpad) and **removed** in the release
that follows its end-of-life. The matrix is reviewed as part of the release
checklist, alongside the dependency review from ADR-0007 — it is a required
step, not optional housekeeping.

As of this ADR (2026-07-18) the matrix is:

```yaml
distro: [noble, resolute, stonking]
```

| Codename   | Version   | Class            | Why targeted                     |
|------------|-----------|------------------|----------------------------------|
| `noble`    | 24.04 LTS | LTS (≥ 2024)     | In standard support              |
| `resolute` | 26.04 LTS | LTS (≥ 2024)     | Current / upcoming LTS           |
| `stonking` | interim   | 9-month interim  | Inside its active support window |

Dropped in this change:

- `jammy` (22.04 LTS) — an LTS, but released **before** 2024; outside the policy.
- `questing` (25.10) — an interim release whose 9-month window has closed.

## Rationale

- **LTS-anchored, like the Java policy.** ADR-0007 targets only LTS Java
  runtimes to spare downstream users a short-lived platform. The same logic
  applies to the OS we ship `.deb`s for: LTS users are the stable majority and
  deserve uninterrupted coverage.
- **The 2024 cutoff bounds the LTS tail.** Supporting *every* LTS ever released
  would mean carrying 22.04, 20.04, and older indefinitely. Anchoring to
  "released in or after 2024" keeps the matrix small while covering the LTS
  releases realistically in production for jPipe's audience.
- **Interim releases are included but expire automatically.** Users who track
  interim Ubuntu still get packages, but the policy makes their removal a
  mechanical consequence of the 9-month clock rather than a judgement call.
- **A written rule makes the YAML edit auditable.** Anyone changing the matrix
  can point to this ADR to justify each add/drop, and reviewers can check the
  edit against the policy rather than against intuition.

## Consequences

- The release checklist gains a step: verify the `publish-ppa` matrix against
  this policy — drop any series that reached end-of-life since the last
  release, and add any newly supported series (including the next LTS's
  development codename).
- Users on an Ubuntu series outside the matrix (e.g. 22.04 `jammy`, or any
  end-of-life interim) no longer receive PPA packages for new releases. The
  Homebrew tap and the raw fat JAR / GitHub Release remain available to them.
- Codenames must be updated by hand each cycle; there is no automatic discovery
  of new Ubuntu series. This is accepted as a low-frequency, low-effort task
  tied to the existing release cadence.
- This policy governs only the *set* of targeted series. The mechanics of how
  each package is built and uploaded remain owned by
  [ADR-0020](0020-tag-triggered-release-pipeline.md).
