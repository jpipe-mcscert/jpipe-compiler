Feature: Source location traceability

  Scenario: directly declared elements carry their source line
    Given the source file "simple_justification.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the model "simple" is declared at line 1
      And the element "c" in model "simple" is declared at line 2
      And the element "s" in model "simple" is declared at line 4
      And the element "e1" in model "simple" is declared at line 7

  Scenario: template elements carry their source line
    Given the source file "override.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the model "t" is declared at line 1
      And the element "c" in model "t" is declared at line 2
      And the element "s" in model "t" is declared at line 3
      And the element "abs" in model "t" is declared at line 5

  Scenario: inlined template elements inherit the template declaration line
    Given the source file "override.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the model "j" is declared at line 9
      And the element "t:c" in model "j" is declared at line 2
      And the element "t:s" in model "j" is declared at line 3
      And the element "t:abs" in model "j" is declared at line 10
