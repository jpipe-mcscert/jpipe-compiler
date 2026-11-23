# ADR-0012: Qualified Ids and Template Expansion

**Date:** 2026-03-27
**Status:** Accepted

## Context

A `Justification` can declare that it `implements` a `Template`. The template
provides a reusable structure — elements and support edges — that the
justification inherits and may partially override. This relationship must support:

1. **Traceability** — given an element in a compiled model, it must be possible
   to determine whether it was declared locally or inherited from a template (and
   from which one).
2. **Operator safety** — operators receive models as inputs and mutate them
   freely. A template may be shared by several justifications; mutations must not
   propagate across models.
3. **Namespace composability** — the `load` statement (`load "file.jd" as ns`)
   brings an external unit into scope under a namespace alias. Element ids must
   remain unambiguous across loaded units.

Three approaches were considered:

- **Clone on operator entry** — elements are mutable, support edges are object
  references. Operators deep-clone all inputs before calling `execute()`. This
  is the approach used on the main branch. It requires a non-trivial
  `deepLink()` walk that reconstructs the support graph on cloned objects, and
  places the cloning burden on every operator.
- **Eager inlining with qualified ids** — when `ImplementsTemplate` executes,
  the template's elements are immediately copied into the justification with
  prefixed ids (`templateName:elementId`). The justification becomes
  self-contained. Operators receive a flat, complete model and need no clone
  machinery.
- **External support graph** — elements are pure value objects (id + label
  only); support edges live in a map on the model. Copying a model is O(n) map
  copy with no graph rewiring. Breaks existing `conclusion.getSupport()`-style
  navigation and requires changing all visitors.

## Decision

Template elements are eagerly expanded into the justification at
`ImplementsTemplate` execution time, using **qualified ids** as the addressing
scheme.

### Qualified id scheme

The grammar's existing `qualified_id` rule (`parts+=ID (COLON parts+=ID)*`) is
the universal addressing form at every level of the model:

| Situation | Id form |
|-----------|---------|
| Element `s` declared directly in model `j` | `s` |
| Element `s` inherited from template `t` into `j` | `t:s` |
| Element `u:s`, already qualified, inherited from `t` into `j` | `u:s` |
| Model `j` loaded under namespace `ns` | `ns:j` |
| Element `s` in `ns:j` | `ns:j:s` |
| Element `s` inherited from `ns:t` into `j` | `ns:t:s` |

Source files always use plain ids within a model body. Qualification is
introduced by the compiler at `ImplementsTemplate` execution time (and by
`load` processing).

### Template inlining rules

When `ImplementsTemplate(modelName, templateName)` executes:

1. Every element in the template (conclusion included) is copied into the
   justification with id `{templateName}:{originalId}`, or with its own id
   when that is already qualified: an element the template inherits from a
   further template keeps the id it has there, and is not qualified twice.
   Each copy records where it comes from (see [Provenance](#provenance)).
2. Every support edge declared in the template is rewritten to use the
   qualified ids of the copied elements.
3. If the justification already contains an element whose plain id matches the
   template element's plain id, the justification's own element takes
   precedence (override). The qualified copy is still added and remains
   reachable by its full id for traceability.
4. The template's conclusion becomes the model's conclusion, under its
   qualified id (`t:c`). A model that implements a template inherits its
   conclusion and cannot declare its own: doing so is an `inherited-conclusion`
   error, reported from the declaration alone, so it does not depend on whether
   the template is declared before or after the model.
5. The template is copied only once it is complete: `implements` waits until no
   pending command still changes the template, its own `implements` included.
   A chain of templates is therefore inherited in full whatever the order in
   which it is declared, and a template that could not be built in full (its
   own `implements` failed or never ran) is never copied.

### Id resolution

`JustificationModel.findById(id)` resolves in two passes:

1. **Exact match** — returns the element whose id equals `id`.
2. **Short-name fallback** — if no exact match, returns the inherited element
   whose id ends with `:{id}`, the inherited conclusion last. A model that
   implements a template may only reach, among inherited elements, the
   `@support` placeholders it overrides: naming the inherited conclusion or a
   strategy is a `reference-into-template` error.

This makes source-level plain ids transparent in `AddSupport` and similar
commands: `e1 supports s` resolves correctly whether `s` is own or inherited.
A relation runs only once every element of its model is declared, so a plain
id binds to the model's own element even when that element is declared after
the relation. When several inherited elements match a plain id (`root:a` and
`mid:a` after a chain of templates), the relation is an `ambiguous-id` error at
its location, and must use the qualified id: picking one would bind it to an
element the author may not have meant.

### Provenance

Each model records, when a copy is made, where each id it holds comes from: an
`Origin`, the model copied from and the id there.

- Template expansion (`inline`) records one per element it copies, the
  conclusion included when it is inherited.
- A composed model records, when its operator call is recorded, one per
  `source:id` its operator copies (ADR-0018): each element of each source, and
  each alias it holds.

Every question about provenance is then a lookup:

- whether an element is the model's own, inherited, or the concrete override of
  an inherited `@support` (`ownElements`, `inheritedElements`,
  `concreteOverrides`, `hasOwnConclusion`, `isInheritedNonSupport`);
- which id a model gives its copy of an ancestor's element (`copyOf`), for the
  template clusters of the diagram and the inherited edges of the JPIPE export;
- which source elements a composed element was built from (the JSON export's
  `origins`);
- which segments of an id name a model, so that the JPIPE export can rename a
  namespaced one;
- in which template an override failed, for a placeholder inherited through a
  chain.

The id is not parsed back for any of these. Once a model loaded under a
namespace has a `:` in its name, the text of an id reads two ways (`b:ns:a:e`,
#202). The ids an author writes as operator arguments are text, and are still
read against the models that exist (`QualifiedIdReadings`, ADR-0018).

### Operator model

With eager inlining, every `JustificationModel` received by an operator is
already flat and self-contained. Operators do not need clone machinery. They
can mutate elements freely without affecting other models or the original
template.

## Rationale

- The `qualified_id` concept is grammar-canonical; using it for element ids
  keeps grammar and model aligned without introducing a new convention.
- Eager inlining at `ImplementsTemplate` time localises the complexity to one
  command. The rest of the pipeline (operators, visitors, exporters) see only
  flat models.
- Traceability is recorded, not parsed. The qualified id stays the address of
  an element, not the record of where it comes from: once a namespaced model's
  name holds a `:`, the id alone is ambiguous (#202). Parsing it also meant
  that every copy mechanism and every reader had to agree on the
  qualification convention, and that each new question about provenance
  added another parser (#209). Recording the origin where the copy is made
  answers them all in one place.
- Ambiguity from multiple templates defining the same plain id is caught as a
  compile-time diagnostic, not a silent resolution.

## Consequences

- `JustificationElement.id()` returns a qualified path string, not a bare
  name. Callers that display or compare ids must be aware that inherited
  elements carry a prefix.
- `ImplementsTemplate.doExecute()` expands the template through
  `JustificationModel.inline`, which copies its elements with prefixed ids,
  rewrites the support edges, records each copy's origin, and sets the model's
  parent.
- `findById` gains a two-pass resolution. `inheritedMatches(id)` lists every
  match of the fallback, so that a relation can refuse an ambiguous plain id.
- The `load` statement follows the same scheme: loading `file.jd as ns`
  prefixes all model ids with `ns:` and their elements become `ns:model:id`.
- Operators are simplified: no `Replicable` interface or deep-clone
  infrastructure is needed.
- `getParent` stays: a model's origins point into the template it implements,
  and the chain of templates is how a placeholder inherited through several of
  them is followed back.
- A new mechanism that copies elements must record their origins, as `inline`
  and `recordComposition` do, or its copies read as the model's own.
