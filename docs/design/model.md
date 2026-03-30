# Model

The `jpipe-model` module defines the internal representation of a compiled jPipe
file. It is structured around four packages.

## Packages

### `model.elements` and `model`

The element hierarchy is rooted at the sealed interface `JustificationElement`.
Elements that appear in both justifications and templates implement `CommonElement`;
`AbstractSupport` is template-only.

Support relationships are encoded directly via typed fields and one marker interface:

- **`SupportLeaf`** — marker for elements that can act as direct supporters of a strategy
  (`SubConclusion`, `Evidence`, `AbstractSupport`).
- `Conclusion` and `SubConclusion` each hold a single `supporter : Strategy`. Cardinality is enforced via `addSupport` which prevents multiple assignments.
- `Strategy` holds a list of supporters (`List<SupportLeaf>`). Multiple elements can support a single strategy (e.g., shared evidence or convergence).

`JustificationModel<E>` is the sealed base for `Justification` (accepts only
`CommonElement`) and `Template` (accepts any `JustificationElement`). A `Unit`
is the root produced by compiling one `.jd` file.

```plantuml
@startuml model

skinparam packageStyle rectangle
skinparam classAttributeIconSize 0
hide empty members

package "model.elements" {

  interface JustificationElement <<sealed>> {
    + id() : String
    + label() : String
    + accept(JustificationVisitor<R>) : R
  }

  interface CommonElement <<sealed>>
  interface SupportLeaf <<marker>>

  class Conclusion {
    - id : String
    - label : String
    - supporter : Strategy
    + addSupport(Strategy) : void
    + getSupport() : Optional<Strategy>
  }

  class SubConclusion {
    - id : String
    - label : String
    - supporter : Strategy
    + addSupport(Strategy) : void
    + getSupport() : Optional<Strategy>
  }

  class Strategy {
    - id : String
    - label : String
    - supporters : List<SupportLeaf>
    + addSupport(SupportLeaf) : void
    + getSupports() : List<SupportLeaf>
    + replaceSupport(SupportLeaf, SupportLeaf) : void
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

  Conclusion    --> "0..1" Strategy    : supporter
  SubConclusion --> "0..1" Strategy    : supporter
  Strategy      --> "*" SupportLeaf : supporters
}

package "model" {

  abstract class JustificationModel<E extends JustificationElement> {
    - name : String
    - conclusion : Conclusion
    - parent : Template
    - elements : List<E>
    + getName() : String
    + setConclusion(Conclusion) : void
    + conclusion() : Optional<Conclusion>
    + getParent() : Optional<Template>
    + setParent(Template) : void
    + addElement(E) : void
    + getElements() : List<E>
    + findById(String) : Optional<E>
    + elementsOfType(Class<T>) : List<T>
    + subConclusions() : List<SubConclusion>
    + strategies() : List<Strategy>
    + evidence() : List<Evidence>
    + accept(JustificationVisitor<R>) : R
    + inline(Template, String) : void
  }

  class Justification
  class Template {
    + abstractSupports() : List<AbstractSupport>
  }

  class Unit {
    - source : String
    - models : Map<String, JustificationModel>
    - locations : Map<String, SourceLocation>
    + getSource() : String
    + add(JustificationModel) : void
    + getModels() : Collection<JustificationModel>
    + findModel(String) : Optional<JustificationModel>
    + get(String) : JustificationModel
    + addInto(String, JustificationElement) : void
    + removeFrom(String, String) : void
    + justifications() : List<Justification>
    + templates() : List<Template>
    + recordLocation(String, SourceLocation) : void
    + locationOf(String) : SourceLocation
    + accept(JustificationVisitor<R>) : R
  }

  JustificationModel <|-- Justification
  JustificationModel <|-- Template

  Unit o-- JustificationModel : models
  Template --o JustificationModel : parent

  JustificationModel --> Conclusion : conclusion
}

@enduml
```

### `commands` / `commands.creation`

Model construction uses the Command pattern. `RegularCommand` is the base for all
concrete commands. The `ExecutionEngine` handles deferred execution (commands whose
condition is not yet met), macro expansion, and deadlock detection.

```plantuml
@startuml commands

skinparam packageStyle rectangle
skinparam classAttributeIconSize 0
hide empty members

' Stub
class Unit

package "commands" {

  interface Command {
    + condition() : Predicate<Unit>
    + execute(Unit) : void
  }

  abstract class RegularCommand {
    # {abstract} doExecute(Unit) : void
  }

  interface MacroCommand {
    + expand(Unit) : List<Command>
  }

  class ExecutionEngine {
    + spawn(String, List<Command>) : Unit
    + enrich(Unit, List<Command>) : Unit
    + totalDeferrals() : int
  }

  Command <|.. RegularCommand
  Command <|.. MacroCommand
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

@enduml
```

### `visitor`

`JustificationVisitor<R>` provides a typed traversal over the full element
hierarchy. All model nodes implement `accept`.

```plantuml
@startuml visitor

skinparam packageStyle rectangle
skinparam classAttributeIconSize 0
hide empty members

' Stubs for linked types
class Unit
interface JustificationElement <<sealed>>
abstract class JustificationModel

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
