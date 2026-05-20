# ADR-0021: Pin Runtime Binary to Declared Dependency Version

**Date:** 2026-05-20
**Status:** Accepted

## Context

jPipe is distributed through two packaging channels, each of which declares Java 25
as a runtime dependency and installs a launcher script (`bin/jpipe`) that contains
an `@@JAVA@@` placeholder resolved at package-install time:

- **Homebrew** — `jpipe.rb` declares `depends_on "openjdk@25"` and installs the
  launcher via the formula's `install` step.
- **Debian/Ubuntu PPA** — `debian/control` declares `openjdk-25-jre[-headless]`
  and installs the launcher via `debian/rules`.

In both cases the `@@JAVA@@` placeholder was previously replaced with the bare
`java` command, which resolves to whichever JVM is the current system default —
not necessarily Java 25. On a machine with multiple JDKs installed this silently
runs jPipe under the wrong runtime version, defeating the version guarantee that
the declared dependency is meant to provide.

## Decision

Installation scripts must invoke the **exact binary provided by the declared
dependency**, not a system default alias. Each packaging channel uses the
resolution mechanism native to its package manager:

**Homebrew** — the formula's `install` step replaces `@@JAVA@@` using Homebrew's
own reference to the declared dependency:

```ruby
inreplace bin/"jpipe", "@@JAVA@@", Formula["openjdk@25"].opt_bin/"java"
```

`Formula["openjdk@25"].opt_bin` is the canonical Homebrew path for the declared
dependency's binaries. It is architecture-aware and does not depend on PATH or any
system-level alternative.

**Debian/Ubuntu** — the dependency is tightened from `openjdk-25-jre` to
`openjdk-25-jre-headless` (the sub-package that owns the `java` binary, and the
minimal correct dependency for a CLI tool with no GUI components). The `@@JAVA@@`
placeholder is resolved at **install time** by a `debian/postinst` script:

```sh
JAVA_BIN=$(update-alternatives --list java | grep 'java-25' | head -1)
sed -i "s|@@JAVA@@|$JAVA_BIN|g" /usr/bin/jpipe
```

`update-alternatives` is the authoritative registry for runtime binary alternatives
on Debian/Ubuntu. Querying it avoids hardcoding an architecture-specific path (e.g.
`/usr/lib/jvm/java-25-openjdk-amd64/bin/java`) while remaining pinned to the
declared version. The substitution happens at install time, where the dependency is
guaranteed present, rather than at build time, where it may not be installed.

## Rationale

- **Correctness over convenience.** The bare `java` alias binds the launcher to a
  global setting that can change independently of the package. The declared
  dependency is meaningless if the launcher ignores it.
- **Each channel has a native resolution mechanism.** Homebrew provides
  `Formula[...].opt_bin`; Debian/Ubuntu provides `update-alternatives`. Using
  these avoids fragile path guessing and keeps the solution idiomatic for each
  platform.
- **Headless is the correct Debian dependency.** `openjdk-25-jre` adds AWT/Swing
  display libraries that jPipe, as a CLI compiler, does not use. Depending on the
  minimal sufficient package reduces the installation footprint.

## Consequences

- `debian/postinst` is introduced; the `@@JAVA@@` sed line is removed from
  `debian/rules`.
- The Homebrew formula in `jpipe-mcscert/homebrew-mcscert` must use
  `Formula["openjdk@25"].opt_bin/"java"` in its `inreplace` call rather than the
  string `"java"`.
- Any future packaging format (RPM `%post`, AppImage, etc.) must apply the same
  principle: resolve the runtime binary path from the package manager's own
  registry rather than using an unqualified command name.
- The launcher's pre-flight `which java` check remains as a developer convenience
  for running the script outside a packaged installation; it does not affect the
  installed path.
