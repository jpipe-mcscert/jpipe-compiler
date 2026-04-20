# ADR-0008: Visitor Pattern for Model Traversal

**Date:** 2026-03-26
**Status:** Accepted

## Context

Consumers of the compiled model (pretty-printers, exporters, validators, linters)
need to traverse the full element hierarchy without coupling their logic to the
model's internal structure. Several traversal strategies were considered:

- **Direct access** — consumers call getters on `Unit`, `JustificationModel`, and
  each element type. Simple, but scatters traversal logic across consumers and
  requires each consumer to know the full type hierarchy.
- **Reflection / instanceof chains** — type-switch on `JustificationElement`.
  Concise at the call site but brittle: the compiler cannot enforce that all
  element types are handled, and adding a new element type silently breaks
  existing consumers.
- **Visitor pattern** — a `JustificationVisitor<R>` interface declares one
  `visit` overload per node type. Each model node implements `accept`. The
  compiler enforces exhaustiveness; new element types require all visitors to
  be updated.

## Decision

All traversal of the compiled model uses the Visitor pattern.

- `JustificationVisitor<R>` (`ca.mcscert.jpipe.visitor`) declares a typed
  `visit` method for every node type: `Unit`, `Justification`, `Template`,
  `Conclusion`, `SubConclusion`, `Strategy`, `Evidence`, and `AbstractSupport`.
- Every model node (`JustificationElement`, `JustificationModel`, `Unit`)
  exposes an `accept(JustificationVisitor<R>) : R` method. The base
  implementations in `JustificationElement` and `JustificationModel` handle
  recursive descent; concrete classes delegate to the correct `visit` overload
  via `visitSelf`.
- No `Visitable` marker interface is introduced. The `accept` contract is
  declared directly on each root type (`JustificationElement`,
  `JustificationModel`, `Unit`) because no code needs to treat these three
  types uniformly via a shared supertype.

## Rationale

- Exhaustiveness is enforced at compile time: adding a new element type
  requires a new `visit` overload, which causes a compilation error in every
  existing visitor implementation.
- The return type parameter `R` keeps visitors pure and composable — a visitor
  can produce a string, a validation result, or a transformed model without
  side effects.
- Omitting a `Visitable` interface follows the same reasoning as the removal of
  `Supportable`: no call site holds a reference typed as "some visitable node",
  so a shared interface would document a constraint that nothing enforces.

## Consequences

- Every new element type added to `model.elements` must be accompanied by a
  new `visit` overload in `JustificationVisitor` and an `accept` implementation
  on the element class.
- Consumers that do not care about all node types must still provide a `visit`
  implementation for each; a default no-op or identity base class may be
  introduced as convenience if the number of consumers grows.
- `accept` on `JustificationModel` and `Unit` drives recursive descent
  automatically; visitors do not need to manage traversal order themselves.
