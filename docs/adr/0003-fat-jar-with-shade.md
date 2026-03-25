# ADR-0003: Fat JAR with Maven Shade

**Date:** 2026-03-25
**Status:** Accepted

## Context

jPipe needs to be distributed as a runnable artifact for Homebrew and apt. Options considered:

- **Maven Shade**: merges all dependencies into a single fat JAR
- **Maven Assembly**: manual fat JAR assembly, more configuration, less smart about resource merging
- **jlink + jpackage**: creates a self-contained runtime image with no JDK requirement, produces native installers (`.dmg`, `.deb`, `.msi`), but requires full JPMS modularization (`module-info.java` per module)

## Decision

Use Maven Shade in `jpipe-cli` to produce the fat JAR, accepting the cosmetic `META-INF/MANIFEST.MF` overlap warning which cannot be suppressed but is harmless.

## Rationale

- Shade is well-maintained and requires no additional modularization work
- The `MANIFEST.MF` warning is a known Shade quirk with no runtime impact
- JPMS modularization of all dependencies is non-trivial at this stage

## Consequences

- Users need Java 25 installed to run jPipe. Homebrew and apt packages will declare it as a dependency.
- The fat JAR is produced in `jpipe-cli/target/`.
- A future ADR should revisit jlink + jpackage when the distribution pipeline is built, as it would eliminate the Java runtime dependency and produce proper native installers.
