# jpipe-cli

Command-line interface and fat JAR entry point. Exposes three PicoCLI
subcommands (`process`, `diagnostic`, `doctor`) and packages all modules into
a single executable JAR via Maven Shade.

## Design

`Main` is the PicoCLI root command. `process` (the default subcommand) and
`diagnostic` both extend `InputOutputCommand`, which handles stream resolution,
logo display, and error reporting. `Doctor` probes external tool availability
(`dot` / Graphviz) before image-format compilation. See
[docs/design/cli.md](../docs/design/cli.md) for the full class diagram and
per-command documentation.

## Dependencies

- `jpipe-compiler` — for `CompilerFactory`, `Compiler`, `CompilationConfig`,
  and `Format`.

(Transitively depends on all other `jpipe-*` modules.)

## Build & test

```bash
# Build the fat JAR
mvn package -pl jpipe-cli --also-make

# Run
java -jar target/jpipe-cli-*.jar --help

# Build it and smoke-test the JAR itself
mvn verify -pl jpipe-cli --also-make
```

The fat JAR is the only distributable artefact; all other modules are
internal. Because it is assembled after the tests run, packaging mistakes are
invisible to them: `ShadedJarIT` therefore runs the built JAR through each
output path, and is what `verify` adds over `package`.
