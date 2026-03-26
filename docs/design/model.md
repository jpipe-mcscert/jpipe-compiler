# Model

The `jpipe-model` module defines the internal representation of a compiled jPipe
file. It is structured around four packages.

## Class diagram

```plantuml
@startuml model

skinparam packageStyle rectangle
skinparam classAttributeIconSize 0
hide empty members

' ─────────────────────────────────────────────
' Elements
' ─────────────────────────────────────────────

package "model.elements" {

  interface JustificationElement <<sealed>> {
    + id() : String
    + label() : String
    + accept(JustificationVisitor<R>) : R
  }

  interface CommonElement <<sealed>>
  interface SupportLeaf <<marker>>

  interface Supportable<T> {
    + addSupport(T) : void
    + getSupporters() : List<T>
  }

  class Conclusion {
    - id : String
    - label : String
    - supporters : List<Strategy>
  }

  class SubConclusion {
    - id : String
    - label : String
    - supporters : List<Strategy>
  }

  class Strategy {
    - id : String
    - label : String
    - supporters : List<SupportLeaf>
  }

  class Evidence <<record>> {
    + id : String
    + label : String
  }

  class AbstractSupport <<record>> {
    + id : String
    + label : String
  }

  JustificationElement <|.. CommonElement
  JustificationElement <|.. AbstractSupport

  CommonElement <|.. Conclusion
  CommonElement <|.. SubConclusion
  CommonElement <|.. Strategy
  CommonElement <|.. Evidence

  SupportLeaf <|.. SubConclusion
  SupportLeaf <|.. Evidence
  SupportLeaf <|.. AbstractSupport

  Supportable <|.. Conclusion     : <Strategy>
  Supportable <|.. SubConclusion  : <Strategy>
  Supportable <|.. Strategy       : <SupportLeaf>
}

' ─────────────────────────────────────────────
' Model
' ─────────────────────────────────────────────

package "model" {

  abstract class JustificationModel<E extends JustificationElement> {
    - name : String
    - parent : Template
    - elements : List<E>
    + getName() : String
    + getParent() : Optional<Template>
    + setParent(Template) : void
    + addElement(E) : void
    + getElements() : List<E>
    + findById(String) : Optional<E>
    + elementsOfType(Class<T>) : List<T>
    + accept(JustificationVisitor<R>) : R
  }

  class Justification {
    + conclusions() : List<Conclusion>
    + subConclusions() : List<SubConclusion>
    + strategies() : List<Strategy>
    + evidence() : List<Evidence>
  }

  class Template {
    + conclusions() : List<Conclusion>
    + subConclusions() : List<SubConclusion>
    + strategies() : List<Strategy>
    + evidence() : List<Evidence>
    + abstractSupports() : List<AbstractSupport>
  }

  class Unit {
    - source : String
    - models : Map<String, JustificationModel>
    + getSource() : String
    + add(JustificationModel) : void
    + getModels() : Collection<JustificationModel>
    + findModel(String) : Optional<JustificationModel>
    + get(String) : JustificationModel
    + addInto(String, JustificationElement) : void
    + justifications() : List<Justification>
    + templates() : List<Template>
    + accept(JustificationVisitor<R>) : R
  }

  JustificationModel <|-- Justification
  JustificationModel <|-- Template

  Unit o-- JustificationModel : models

  Justification ..> Conclusion
  Justification ..> SubConclusion
  Justification ..> Strategy
  Justification ..> Evidence

  Template ..> Conclusion
  Template ..> SubConclusion
  Template ..> Strategy
  Template ..> Evidence
  Template ..> AbstractSupport

  Template --o JustificationModel : parent
}

' ─────────────────────────────────────────────
' Commands
' ─────────────────────────────────────────────

package "commands" {

  interface Command {
    + condition() : Function<Unit, Boolean>
    + execute(Unit) : void
  }

  abstract class RegularCommand

  class ExecutionEngine {
    + spawn(String, List<Command>) : Unit
    + enrich(Unit, List<Command>) : Unit
  }

  Command <|.. RegularCommand
  ExecutionEngine ..> Command
  ExecutionEngine ..> Unit
}

package "commands.creation" {
  class CreateJustification
  class CreateTemplate
  class CreateConclusion
  class CreateSubConclusion
  class CreateStrategy
  class CreateEvidence
  class CreateAbstractSupport

  RegularCommand <|-- CreateJustification
  RegularCommand <|-- CreateTemplate
  RegularCommand <|-- CreateConclusion
  RegularCommand <|-- CreateSubConclusion
  RegularCommand <|-- CreateStrategy
  RegularCommand <|-- CreateEvidence
  RegularCommand <|-- CreateAbstractSupport
}

' ─────────────────────────────────────────────
' Visitor
' ─────────────────────────────────────────────

package "visitor" {
  interface JustificationVisitor<R> {
    + visit(Unit) : R
    + visit(Justification) : R
    + visit(Template) : R
    + visit(Conclusion) : R
    + visit(SubConclusion) : R
    + visit(Strategy) : R
    + visit(Evidence) : R
    + visit(AbstractSupport) : R
  }
}

JustificationElement ..> JustificationVisitor : accept
JustificationModel   ..> JustificationVisitor : accept
Unit                 ..> JustificationVisitor : accept

@enduml
```

## Packages

### `model.elements`

The element hierarchy is rooted at the sealed interface `JustificationElement`.
Elements that appear in both justifications and templates implement `CommonElement`;
`AbstractSupport` is template-only.

Support relationships are encoded via two interfaces:

- **`SupportLeaf`** — marker for elements that can act as direct supporters
  (`SubConclusion`, `Evidence`, `AbstractSupport`).
- **`Supportable<T>`** — implemented by elements that *receive* support, with `T`
  constraining the allowed supporter type.

### `model`

`JustificationModel<E>` is the sealed base for `Justification` (accepts only
`CommonElement`) and `Template` (accepts any `JustificationElement`). A `Unit`
is the root produced by compiling one `.jd` file.

### `commands` / `commands.creation`

Model construction uses the Command pattern. `RegularCommand` is the base for all concrete commands. The `ExecutionEngine`
handles deferred execution (commands whose condition is not yet met) and deadlock
detection.

### `visitor`

`JustificationVisitor<R>` provides a typed traversal over the full element
hierarchy. All model nodes implement `accept`.
