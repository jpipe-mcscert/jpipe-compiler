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
| Model `j` loaded under namespace `ns` | `ns:j` |
| Element `s` in `ns:j` | `ns:j:s` |
| Element `s` inherited from `ns:t` into `j` | `ns:t:s` |

Source files always use plain ids within a model body. Qualification is
introduced by the compiler at `ImplementsTemplate` execution time (and by
`load` processing).

### Template inlining rules

When `ImplementsTemplate(modelName, templateName)` executes:

1. Every element in the template (conclusion included) is copied into the
   justification with id `{templateName}:{originalId}`.
2. Every support edge declared in the template is rewritten to use the
   qualified ids of the copied elements.
3. If the justification already contains an element whose plain id matches the
   template element's plain id, the justification's own element takes
   precedence (override). The qualified copy is still added and remains
   reachable by its full id for traceability.
4. The template's conclusion is copied as an ordinary element (not set as the
   justification's conclusion). The justification must declare its own
   conclusion; the template conclusion becomes part of the inherited structure.

### Id resolution

`JustificationModel.findById(id)` resolves in two passes:

1. **Exact match** — returns the element whose id equals `id`.
2. **Short-name fallback** — if no exact match, returns the inherited element
   whose id ends with `:{id}`. If multiple inherited elements match (two
   templates both define `s`), a `FATAL` diagnostic is raised.

This makes source-level plain ids transparent in `AddSupport` and similar
commands: `e1 supports s` resolves correctly whether `s` is own or inherited.

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
- Traceability is structural: the id prefix carries full provenance with no
  extra metadata field.
- Ambiguity from multiple templates defining the same plain id is caught as a
  compile-time diagnostic, not a silent resolution.

## Consequences

- `JustificationElement.id()` returns a qualified path string, not a bare
  name. Callers that display or compare ids must be aware that inherited
  elements carry a prefix.
- `ImplementsTemplate.doExecute()` must copy elements with prefixed ids and
  rewrite support edges. It currently calls `setParent(template)` only; that
  call is replaced by the expansion logic.
- `findById` gains a two-pass resolution. Ambiguity is a `FATAL` diagnostic
  requiring a `CompilationContext` parameter.
- The `load` statement follows the same scheme: loading `file.jd as ns`
  prefixes all model ids with `ns:` and their elements become `ns:model:id`.
- Operators are simplified: no `Replicable` interface or deep-clone
  infrastructure is needed.
- The `setParent` / `getParent` API on `JustificationModel` becomes redundant
  once expansion is complete and can be removed.
