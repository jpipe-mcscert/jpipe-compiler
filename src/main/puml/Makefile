.DEFAULT_GOAL := pdf

## Diagram compilation : using PlantUML

PLANTUML_EXEC = plantuml
PUML = $(PLANTUML_EXEC) -charset UTF-8

SOURCES := $(shell find * -type f -name "*.puml")

PNGS = $(SOURCES:.puml=.png)
SVGS = $(SOURCES:.puml=.svg)
PDFS = $(SOURCES:.puml=.pdf)

%.png: %.puml
	$(PUML) ./$<

%.svg: %.puml
	$(PUML) -tsvg ./$<

%.pdf: %.svg
	rsvg-convert -f pdf -o $@ $<

testenv:
	$(PUML) -testdot

png: $(PNGS)
pdf: $(PDFS)

## Clean
clean:
	rm -rf $(PDFS) $(SVGS)

clobber: clean
	rm -rf $(PNGS)