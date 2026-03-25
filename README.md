# jPipe - Justified Pipelines

<div align="center">

  <img alt="tool logo" src="./docs/logo.svg" width="300">

</div>

The jPipe environment supports the definition of justification to support software maintenance activities. The name comes from "justified pipelines", as the key idea is to design an environment supporting the justification of CI/CD pipelines by design.

## General Information

<div align="center" markdown="1">

![McScert logo](./docs/sponsors/mcscert.svg)

</div>

- Principal Investigator:
  - [Sébastien Mosser](https://mosser.github.io/), McSCert, McMaster University.
- Active Contributors:
  - [Kalvin Thuan-Phong Khuu](https://kalvinkhuu.github.io/), McSCert, McMaster University. PhD Student.
- Contributors:
  - [Nirmal Chaudhari](https://www.linkedin.com/in/nirmal2003/), McSCert, McMaster University. Undergraduate Research Assistant.
  - [Aaron Loh](https://www.linkedin.com/in/aaron-loh26/), McSCert, McMaster University. Undergraduate Research Assistant.
  - [Deesha Patel](https://www.linkedin.com/in/deeshupatel/), McSCert, McMaster University. Master Student.
  - [Corinne Pulgar](https://www.linkedin.com/in/corinne-pulgar-12a58190/), École de Technologie Supérieure (ETS). Master Student

## Repository organization

- `jpipe-lang`: ANTLR4 grammar and generated lexer/parser
- `jpipe-model`: domain model (justification elements, symbol table)
- `jpipe-operators`: composition operator extension point and built-in operators
- `jpipe-compiler`: compiler pipeline (parsing, model building, validation, export)
- `jpipe-cli`: command-line interface and fat JAR entry point
- `docs`: technical documentation and architecture decision records
- `templates`: templates used for distributing the compiler with Homebrew and apt

## Developer Setup

### Required tools

| Tool | Version | Purpose |
|---|---|---|
| [JDK](https://adoptium.net/) | 25 | Compilation and runtime |
| [Maven](https://maven.apache.org/) | 3.x | Build tool |
| [adr-tools](https://github.com/npryce/adr-tools) | latest | Browsing and creating architecture decision records |

### Optional tools

| Tool | Version | Purpose |
|---|---|---|
| [MkDocs Material](https://squidfunk.github.io/mkdocs-material/) | latest | Previewing the documentation site locally (`mkdocs serve`) |

### Building

```bash
mvn package
```

The fat JAR is produced in `jpipe-cli/target/`.

### Code style

Formatting is enforced by Spotless (Google Java Format) and Checkstyle. To auto-format before committing:

```bash
mvn spotless:apply
```

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
