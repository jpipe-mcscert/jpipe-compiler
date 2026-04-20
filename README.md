# jPipe - Justified Pipelines

<div align="center">
  <img alt="jPipe logo" src="./docs/logo.svg" height="100" style="vertical-align:middle">
  &nbsp;&nbsp;&nbsp;&nbsp;
  <img alt="McSCert logo" src="./docs/sponsors/mcscert.svg" height="100" style="vertical-align:middle">
</div>

<br>

The jPipe environment supports the definition of justification to support software maintenance activities. The name comes from "justified pipelines", as the key idea is to design an environment supporting the justification of CI/CD pipelines by design.

## General Information

- Principal Investigator:
  - [Sébastien Mosser](https://mosser.github.io/), McSCert, McMaster University.
- Active Contributors:
  - [Kalvin Thuan-Phong Khuu](https://kalvinkhuu.github.io/), McSCert, McMaster University. PhD Student.
- Contributors:
  - [Nirmal Chaudhari](https://www.linkedin.com/in/nirmal2003/), McSCert, McMaster University. Undergraduate Research Assistant.
  - [Aaron Loh](https://www.linkedin.com/in/aaron-loh26/), McSCert, McMaster University. Undergraduate Research Assistant.
  - [Deesha Patel](https://www.linkedin.com/in/deeshupatel/), McSCert, McMaster University. Master Student.
  - [Corinne Pulgar](https://www.linkedin.com/in/corinne-pulgar-12a58190/), École de Technologie Supérieure (ETS). Master Student.

### Repository organization

- `jpipe-lang`: ANTLR4 grammar and generated lexer/parser
- `jpipe-model`: domain model (justification elements, symbol table)
- `jpipe-operators`: composition operator extension point and built-in operators
- `jpipe-compiler`: compiler pipeline (parsing, model building, validation, export)
- `jpipe-cli`: command-line interface and fat JAR entry point
- `docs`: technical documentation and architecture decision records
- `templates`: templates used for distributing the compiler with Homebrew and apt

### Developer Setup

#### Required tools

| Tool | Version | Purpose |
|---|---|---|
| [JDK](https://adoptium.net/) | 25 | Compilation and runtime |
| [Maven](https://maven.apache.org/) | 3.x | Build tool |
| [adr-tools](https://github.com/npryce/adr-tools) | latest | Browsing and creating architecture decision records |

#### Optional tools

| Tool | Version | Purpose |
|---|---|---|
| [MkDocs Material](https://squidfunk.github.io/mkdocs-material/) | latest | Previewing the documentation site locally (`mkdocs serve`) |

#### Building

```bash
mvn package
```

The fat JAR is produced in `jpipe-cli/target/`.

#### Code style

Formatting is enforced by Spotless (Google Java Format) and Checkstyle. To auto-format before committing:

```bash
mvn spotless:apply
```

#### Build output / shade warnings

The fat JAR build (`jpipe-cli`) merges many dependencies and would normally emit overlap warnings from the Maven Shade plugin. These are suppressed by default via `.mvn/jvm.config`. To re-enable them for a single run:

```bash
mvn package -Dorg.slf4j.simpleLogger.log.org.apache.maven.plugins.shade.DefaultShader=warn
```

## Usage

### Running the compiler

After building, the fat JAR is at `jpipe-cli/target/jpipe-cli-*.jar`. The
`process` subcommand is the default, so these two invocations are equivalent:

```bash
java -jar jpipe-cli/target/jpipe-cli-*.jar -i my.jd -d MyModel -f dot
java -jar jpipe-cli/target/jpipe-cli-*.jar process -i my.jd -d MyModel -f dot
```

### Common flags

| Flag | Short | Description |
|------|-------|-------------|
| `--input FILE` | `-i` | Input `.jd` source file (default: stdin) |
| `--output FILE` | `-o` | Output file (default: stdout) |
| `--diagram NAME` | `-d` | Name of the model to export (required) |
| `--format FORMAT` | `-f` | Output format — see table below (default: `JPIPE`) |
| `--headless` | | Suppress the logo banner |
| `--log-level LEVEL` | | Log verbosity: `OFF` `ERROR` `WARN` `INFO` `DEBUG` `TRACE` |

### Output formats

| Format | Description | Requires Graphviz |
|--------|-------------|:-----------------:|
| `JPIPE` | Canonical jPipe source (round-trip) | No |
| `DOT` | Graphviz DOT source | No |
| `PNG` | Rendered PNG image | Yes |
| `JPEG` | Rendered JPEG image | Yes |
| `SVG` | Rendered SVG image | Yes |
| `JSON` | JSON model dump | No |
| `PYTHON` | Python object model | No |

Install [Graphviz](https://graphviz.org/) and run `jpipe doctor` to verify that
the `dot` binary is on your `PATH` before using image formats.

### Other subcommands

```bash
# Parse and validate without exporting — prints diagnostics, statistics, and the symbol table
java -jar jpipe-cli/target/jpipe-cli-*.jar diagnostic -i my.jd

# Check runtime dependencies (Graphviz)
java -jar jpipe-cli/target/jpipe-cli-*.jar doctor
```

### Minimal `.jd` example

```
justification MyModel {
    conclusion  c  is "Our claim"
    strategy    s  is "Argument"
    evidence    e1 is "Evidence A"
    evidence    e2 is "Evidence B"

    s  supports c
    e1 supports s
    e2 supports s
}
```

Compile it to a DOT diagram:

```bash
java -jar jpipe-cli/target/jpipe-cli-*.jar -i my.jd -d MyModel -f dot -o my.dot
dot -Tpng my.dot -o my.png
```

More examples are in the [`examples/`](examples/) directory.

## How to cite?

```bibtex
@software{mcscert:jpipe,
  author = {Mosser, Sébastien and Khuu, Kalvin Thuan-Phong and Chaudhari, Nirmal and Loh, Aaron and Patel, Deesha and Pulgar, Corinne},
  license = {MIT},
  title = {{jPipe}},
  url = {https://github.com/jpipe-mcscert/jpipe-compiler}
}
```

## How to contribute?

Found a bug, or want to add a cool feature? Feel free to fork this repository and send a pull request.

If you're interested in contributing to the research effort related to jPipe, feel free to contact the PI: [Dr. Sébastien Mosser](mailto:mossers@mcmaster.ca).

**We do have undergrad summer internships available to contribute to the compiler, as
well as MASc and PhD positions in Software Engineering at Mac.**

### AI assistance policy

Parts of this codebase were developed with the assistance of [Claude](https://claude.ai) (Anthropic), an AI coding assistant. We are transparent about this use and welcome AI-assisted contributions, subject to the following conditions:

- Pull requests must not be 100% AI-generated. Every contribution must reflect the understanding and judgement of a human author.
- Human authors are fully responsible for the correctness, quality, and appropriateness of their contributions, regardless of whether AI tools were used in their preparation.
- Reviewers may ask contributors to explain any part of their submission.

## Sponsors

We acknowledge the support of the _Natural Sciences and Engineering Research Council of Canada_
(NSERC), as well as McMaster _Excellence in Research Award_ (EREA) from the Faculty of Engineering.

<div align="center">
  <img alt="NSERC logo" src="./docs/sponsors/nserc.svg" width="300">
</div>
