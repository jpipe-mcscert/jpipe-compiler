Feature: Compiling template source files

  Scenario: template with abstract support
    Given the source file "template.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "t"
    Then it has a conclusion with id "c" and label "The system is correct"
      And it has a strategy with id "s" and label "Testing"
      And it has an abstract support with id "abs" and label "Abstract support"
      And the strategy "s" supports the conclusion "c"
      And the abstract support "abs" supports the strategy "s"

  Scenario: justification overrides abstract support with evidence
    Given the source file "override.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "j"
    Then it has evidence with id "t:abs" and label "Test results"
      And the evidence "t:abs" supports the strategy "t:s"
