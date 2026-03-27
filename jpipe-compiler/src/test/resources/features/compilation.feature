Feature: Compiling jPipe source files into a unit

  Scenario: standalone justification
    Given the source file "simple_justification.jd"
    When I compile it into a unit
    Then the compilation succeeds
    And the unit contains a justification named "simple"
    And the justification "simple" has a conclusion with id "c" and label "The system is correct"
    And the justification "simple" has a strategy with id "s" and label "Testing"
    And the justification "simple" has evidence with id "e1" and label "Test results"
    And in justification "simple" the strategy "s" supports the conclusion "c"
    And in justification "simple" the evidence "e1" supports the strategy "s"
