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
[gen_railroad.py](gen_railroad.py). To regenerate them:

```bash
cd jpipe-lang
pipenv install --deploy
pipenv run python3 gen_railroad.py
```

## Dependencies

None. This is the root of the module dependency graph.

## Build & test

```bash
mvn test -pl jpipe-lang
```
