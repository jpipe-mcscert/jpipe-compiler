Feature: Compiling jPipe source files into a unit

  Scenario: standalone justification
    Given the source file "simple_justification.jd"
    When I compile it into a unit
    Then the compilation succeeds
    When examining justification "simple"
    Then it has a conclusion with id "c" and label "The system is correct"
      And it has a strategy with id "s" and label "Testing"
      And it has evidence with id "e1" and label "Test results"
      And the strategy "s" supports the conclusion "c"
      And the evidence "e1" supports the strategy "s"

  Scenario: missing input file triggers a system error
    Given the source file "does_not_exist.jd"
    When I compile it into a unit
    Then the compilation fails with a system error
