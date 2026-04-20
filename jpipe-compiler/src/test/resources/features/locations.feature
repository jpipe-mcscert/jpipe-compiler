Feature: Source location traceability

  Scenario: directly declared elements carry their source line
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the model "minimal" is declared at line 8
      And the element "c" in model "minimal" is declared at line 9
      And the element "s" in model "minimal" is declared at line 10
      And the element "e" in model "minimal" is declared at line 11

  Scenario: template elements carry their source line
    Given the source file "005_override.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the model "a_template" is declared at line 8
      And the element "c" in model "a_template" is declared at line 9
      And the element "s" in model "a_template" is declared at line 10
      And the element "abs" in model "a_template" is declared at line 11

  Scenario: inlined template elements inherit the template declaration line
    Given the source file "005_override.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the model "immediate" is declared at line 18
      And the element "a_template:c" in model "immediate" is declared at line 9
      And the element "a_template:s" in model "immediate" is declared at line 10
      And the element "a_template:abs" in model "immediate" is declared at line 19
