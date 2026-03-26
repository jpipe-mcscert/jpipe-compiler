# ADR-0004: SonarCloud as Mandatory Quality Gate

**Date:** 2026-03-25
**Status:** Accepted

## Context

As the jPipe compiler grows across multiple modules (`jpipe-lang`, `jpipe-model`, `jpipe-operators`, `jpipe-compiler`, `jpipe-cli`), maintaining code quality and test coverage by convention alone becomes unreliable. Existing tooling (Checkstyle, Spotless) enforces style but does not track coverage regressions, code smells, or security hotspots over time.

A static analysis platform integrated into CI would give contributors and maintainers continuous, objective feedback on quality trends. SonarCloud (cloud-hosted SonarQube) was selected over alternatives such as Codacy or self-hosted SonarQube because it is free for open-source projects, has native GitHub integration, and requires no infrastructure to operate.

## Decision

SonarCloud is mandatory for the jPipe project. Analysis runs on every push and pull request via GitHub Actions. Pull requests that fail the SonarCloud Quality Gate must not be merged.

## Rationale

- SonarCloud is free for public open-source repositories — no hosting or licensing cost.
- Native GitHub pull request decoration surfaces issues inline, reducing review friction.
- The Quality Gate acts as an objective, automated enforcer of coverage and smell thresholds, independent of reviewer availability.
- JaCoCo integration (already configured in `pom.xml`) feeds coverage data to SonarCloud with no additional instrumentation.

## Consequences

- A `SONAR_TOKEN` secret must be present in the repository for CI analysis to succeed.
- The SonarCloud Quality Gate is a required status check on protected branches; PRs cannot be merged without a passing gate.
- Contributors must ensure new code maintains or improves coverage; regressions block merges.
