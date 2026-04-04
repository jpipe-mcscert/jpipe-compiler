# ADR-0006: Logging Conventions

**Date:** 2026-03-26
**Status:** Accepted

## Context

The jPipe compiler spans several layers — CLI entry point, parser/listener, compiler orchestration, command scheduling, and model construction — each with distinct observability needs. Without agreed conventions, log levels get assigned arbitrarily and operational output leaks into `System.out`, making it hard to filter signal from noise in production or during debugging.

Log4j2 is already on the classpath (via `log4j-api` / `log4j-core`).

Two structural choices for logger declaration arise:

- **Static logger** — `private static final Logger logger = LogManager.getLogger()`. The no-arg form resolves the owning class via `MethodHandles.lookup()` at the call site. Correct in a concrete class; silently wrong in a base class (always resolves to the base, not the subclass).
- **Instance logger** — `LogManager.getLogger(getClass())`. Resolves to the concrete runtime class. Required whenever a base class logs on behalf of subclasses.

## Decision

### No `System.out` / `System.err` for operational output

All operational output (parse events, compilation progress, errors) goes through the logging framework. `System.out` and `System.err` are reserved for the CLI's user-facing output (e.g. final results rendered to the terminal), not for internal pipeline events.

**Exception — compilation diagnostics:** `ChainCompiler.printDiagnostics()` writes accumulated `ERROR`/`FATAL` diagnostics to `System.err` at the end of `compile()`. This is intentional: these messages are the user-facing result of compilation (analogous to `gcc` printing errors to stderr) rather than internal pipeline observability. They are not subject to log-level filtering and must always reach the user. New pipeline code that needs to report an error condition should use `CompilationContext` (which feeds `printDiagnostics`), not `System.err` directly.

### Logger declaration

Each class that performs logging declares its own `private` logger:

- **Concrete classes:** `private static final Logger logger = LogManager.getLogger();`
- **Abstract base classes that log on behalf of subclasses:** `private final Logger logger = LogManager.getLogger(getClass());` (instance field, no `static`)

No `protected` logger fields. Subclass logging goes through a template method in the base class.

### Level conventions

| Level   | Meaning | Pipeline examples |
|---------|---------|------------------|
| `ERROR` | The operation has failed in a way that cannot be recovered from within the current scope. | Engine deadlock; unhandled exception during command execution; parse failure on a required file. |
| `WARN`  | Something unexpected happened but execution continues. The result may be degraded or surprising. | A model element overwritten; a deprecated construct encountered in source. |
| `INFO`  | A significant lifecycle event that an operator would care about in production. | Compilation of a file started/finished; a `Unit` produced from a source path. |
| `DEBUG` | Fine-grained execution detail useful during development or troubleshooting. Disabled in production by default. | Each command executed; each parse rule entered with its arguments. |
| `TRACE` | Internal scheduling or algorithmic decisions. Only useful when diagnosing the engine itself. | Defer/execute decisions in `ExecutionEngine`; visitor traversal steps. |

### Per-layer guidance

**CLI (`jpipe-cli`)** — `INFO` on startup (source file, tool version) and on successful exit. `ERROR` on unhandled exceptions before propagating to picocli.

**Compiler (`jpipe-compiler`)** — `INFO` when compilation of a file begins and ends. `DEBUG` for intermediate steps (token stream size, parse tree depth). Parse listeners use `DEBUG` for rule events and `WARN`/`ERROR` for error nodes, replacing any `System.out.println` calls.

**Command execution (`jpipe-model` — `ExecutionEngine`)** — `TRACE` for scheduling decisions (defer/execute). `ERROR` for deadlock and command exceptions.

**Commands (`jpipe-model` — `RegularCommand` subclasses)** — `DEBUG` for each command dispatched, logged uniformly by the base class template method using the command's `toString()` as payload. Individual commands do not log directly.

**Model (`jpipe-model` — elements, justifications)** — `WARN` for guard violations (e.g. attempted parent overwrite). No lower-level logging in the model layer.

**Operators (`jpipe-operators`)** — `INFO` for operator invocation. `ERROR` for operator failures.

## Rationale

- A single framework (Log4j2) with a consistent level vocabulary makes log output filterable by level and by logger name across all modules.
- Banning `System.out` from pipeline internals ensures that log configuration (level filters, appenders, format) fully controls what reaches the operator.
- Instance loggers in base classes are the only way to get the correct class name in log output without each subclass redeclaring its own logger.
- Per-layer guidance removes ambiguity about which level to choose — each layer has a natural home level for its primary events.

## Consequences

- `PrintingListener` (currently uses `System.out.println`) must be updated to use a Log4j2 logger at `DEBUG` for rule events and `ERROR` for error nodes.
- `Compiler` and `Main` must add `INFO` lifecycle logs.
- New commands automatically inherit `DEBUG` logging via `RegularCommand`'s template method; no per-command logging code needed.
- Existing `System.out` calls in pipeline classes are a compliance violation under this ADR and should be treated as bugs.
- **Prerequisite — Shade compatibility (ADR-0003):** The logging conventions in this ADR are only effective in the fat JAR if Log4j2's plugin cache is correctly merged by Shade. Without `Log4j2PluginCacheFileTransformer`, Log4j2 degrades silently and `System.out` workarounds become necessary. Any pipeline code using `System.out` instead of Log4j2 should be treated as a Shade misconfiguration symptom first, a logging convention violation second.
