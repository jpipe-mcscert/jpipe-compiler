# ADR-0014: Colorblind-Safe Colour Palette for DOT Export

**Date:** 2026-03-28
**Status:** Accepted

## Context

The `DotExporter` renders each element type with a distinct fill or border colour
so that readers can quickly identify node roles in a justification diagram. The
original palette was chosen for visual appeal under normal vision:

| Element | Shape | Colour |
|---|---|---|
| Conclusion | rounded rect | `lightgrey` fill |
| SubConclusion | rect | `dodgerblue` border |
| Strategy | hexagon | `palegreen` fill |
| Evidence | note | `lightskyblue2` fill |
| AbstractSupport | dotted rect | no fill |

This palette has accessibility problems. Approximately 8 % of males have
red-green colour vision deficiency (deuteranopia or protanomaly). Under
deuteranopia, `palegreen` shifts to a dull brownish-beige that is nearly
indistinguishable from `lightgrey`, making Strategy nodes visually
indistinguishable from Conclusion nodes. Under protanopia the same collapse
occurs. The two blue tones (`dodgerblue` and `lightskyblue2`) also converge
under tritanopia.

Node shapes already provide a redundant visual encoding that is independent of
colour. Colours should reinforce shape differentiation, not contradict it.

## Decision

**Adopt a palette derived from the Okabe-Ito (2008) colorblind-safe colour
set**, with lightened fill values to maintain WCAG AA contrast with black text
(lightness ≥ 70 %).

| Element | Shape | New colour | Okabe-Ito base |
|---|---|---|---|
| Conclusion | rounded rect | `lightgrey` fill | — (achromatic, unchanged) |
| SubConclusion | rect | `#0072B2` border | Blue |
| Strategy | hexagon | `#F0C27F` fill | Orange (#E69F00), lightened |
| Evidence | note | `#9ECAE1` fill | Sky blue (#56B4E9), lightened |
| AbstractSupport | dotted rect | no fill | — (unchanged) |

The critical change is Strategy: `palegreen` → amber (`#F0C27F`). Amber
(orange-family) is safe under all common forms of colour vision deficiency and
is clearly distinct from the blues used for SubConclusion and Evidence.

All colour values are centralised in five named `NodeStyle` constants in
`DotExporter` under a clearly marked "change shapes and colours here" section.
Hex values are supported alongside X11 named colours via a `dotColor()` helper
that quotes hex strings automatically.

## Rationale

- **Okabe-Ito** is the palette recommended by *Nature Methods* (2011) and is
  designed to remain distinguishable under deuteranopia, protanopia, and
  tritanopia simultaneously.
- Shapes already encode node role; colours are a secondary, redundant cue.
  This means even a complete failure of colour differentiation (achromatopsia)
  does not break diagram comprehension.
- Keeping colours as named constants with a `NodeStyle` record (introduced in
  the same refactoring session) means future palette changes require touching
  exactly five lines, with no risk of missing an occurrence buried in visitor
  code.

## Consequences

- DOT output produced by this version is visually different from previous
  versions. Existing screenshots or reference images in documentation will need
  updating.
- The `NodeStyle` record's `dotColor()` helper handles hex quoting; future
  colour values may be either X11 names or `#RRGGBB` hex strings without
  further changes to `toAttrs()`.
- If a high-contrast or dark-mode theme is ever needed, the same `NodeStyle`
  constants are the single point of change.
