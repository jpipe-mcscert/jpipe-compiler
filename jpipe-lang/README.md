# jpipe-lang

ANTLR4 grammar for the jPipe language. Generates the lexer and parser used
by all other modules to read `.jd` source files.

## Design

The grammar is defined in a single file:
`src/main/antlr4/ca/mcscert/jpipe/lang/JPipe.g4`. The Maven ANTLR4 plugin
generates `JPipeLexer` and `JPipeParser` at build time; these generated
sources are not committed. See [docs/design/language.md](../docs/design/language.md)
for a full language reference with railroad diagrams.

Railroad diagrams in `docs/design/img/` are produced by
[gen_railroad.py](gen_railroad.py). Its dependency is locked with the rest of
the documentation tooling, in [docs/requirements.txt](../docs/requirements.txt).
To regenerate them, from the repository root, in a Python 3.12 virtual
environment:

```bash
pip install --require-hashes --only-binary :all: -r docs/requirements.txt
python3 jpipe-lang/gen_railroad.py
```

## Dependencies

None. This is the root of the module dependency graph.

## Build & test

```bash
mvn test -pl jpipe-lang
```
