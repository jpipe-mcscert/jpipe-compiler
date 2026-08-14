# Diagnostic report JSON Schema

`jpipe diagnostic -f json` emits a document described by a published
[JSON Schema](https://json-schema.org/) (draft 2020-12). The schema is the
contract between the compiler and its consumers: an IDE extension, a CI
dashboard, or any tool that reads the report can validate against it and code
against its guarantees rather than against whatever the compiler happened to
print.

| | |
|---|---|
| **Download** | <https://ace-design.github.io/jpipe/schema/diagnostic-report-v1.schema.json> |
| **`$id`** | `https://ace-design.github.io/jpipe/schema/diagnostic-report-v1.schema.json` |
| **Source of truth** | `jpipe-compiler/src/main/resources/schema/diagnostic-report-v1.schema.json` |
| **Current version** | `schemaVersion: 1` |

## Getting the schema

There is exactly one copy of the file — a packaged compiler resource. It is
reachable three ways:

- **From the classpath**, the cheapest option for a JVM consumer: it ships in
  `jpipe-compiler` and in the fat JAR at
  `/schema/diagnostic-report-v1.schema.json`.

  ```java
  try (InputStream in = getClass()
          .getResourceAsStream("/schema/diagnostic-report-v1.schema.json")) {
      // …
  }
  ```

- **From the documentation site**, at the `$id` URL above. The docs build stages
  the packaged resource into the site; it is not a second copy kept in the
  repository, so it cannot drift.

- **From the repository**, at the source-of-truth path in the table above.

## Validating

The schema is strict: `additionalProperties` is `false` throughout, so an
unexpected key is a violation. Beyond structure it pins several invariants
worth relying on:

- a diagnostic `code` is bare kebab-case, never bracketed — `unknown-model`,
  not `[unknown-model]`;
- `line` and `column` always appear together, or not at all;
- a symbol carries a `location` **iff** it is not `synthesized`;
- a `justification` never has entries in `usedBy`;
- `elements.conclusion` is `0` or `1` — a model owns at most one conclusion.

```bash
# with any draft 2020-12 validator, e.g. check-jsonschema
jpipe diagnostic -i my.jd -f json > report.json
check-jsonschema \
  --schemafile jpipe-compiler/src/main/resources/schema/diagnostic-report-v1.schema.json \
  report.json
```

## Versioning policy

Every document carries `schemaVersion`, so a consumer can branch on it before
reading anything else.

**Any change to the set of members publishes a new schema file
(`…-v2.schema.json`) and increments `schemaVersion`. Adding an optional member
counts.** Earlier schemas stay reachable at their own `$id`, so a consumer
pinned to one keeps validating the documents it was written for.

That rule follows from strictness rather than caution. Because the schema sets
`additionalProperties: false`, a document carrying a member the schema does not
list *fails validation* — so there is no such thing as an addition that is
invisible to a consumer. Telling consumers to "ignore members you don't know"
would not help: their validator rejects the document before their code sees it.

The alternative would be to let the schema permit unknown properties, which
buys silent additions at the cost of the guarantee that makes the schema useful
as a test oracle — a typo'd or duplicated key would then validate cleanly. The
strict reading is the deliberate choice; the versioning rule above is its
consequence.

For consumers, the practical form is:

```jsonc
if (report.schemaVersion !== 1) {
  // a shape this code was not written against — refuse it or handle it
  // explicitly, rather than reading fields that may have changed meaning
}
```

## Where it is enforced

The schema is **not** validated at runtime. The document is built by
straight-line code from a typed `DiagnosticSnapshot`, so no input a user
supplies can make it structurally invalid — only a change to the compiler can,
and that is a build-time concern. Validating on every invocation would spend
work checking our own output, and would leave nothing sensible to do on
failure: aborting a compilation that just succeeded is worse than emitting the
report.

Instead `JsonDiagnosticReportSchemaTest` validates against the schema every
report produced from the unit fixtures **and from every file in `examples/`**,
compiled through the same wiring the CLI uses — including the sources that
abort, which are also asserted to carry their fatal — so code and schema cannot
drift apart without CI noticing. The same test also
asserts that the schema rejects malformed documents, so it cannot quietly decay
into one that accepts anything.

## Note on fatal errors

A *fatal* error — a syntax error, an unresolvable `load` — aborts the
compilation, and the report describes exactly that: one or more diagnostics
with `severity: "fatal"`, `status: "errors"`, an empty `models` array and zeroed
statistics, because nothing could be built. The exit code is 1.

Consumers therefore need no separate path for it: the document has the same
shape as any other, and an aborted compilation is recognised by the severity.
That matters most for editor integrations, since a file being edited is
syntactically broken most of the time.

See [`cli.md`](cli.md#diagnostic) for the command itself.
