Feature: Compiling justification source files

  Scenario: standalone justification
    Given the source file "simple_justification.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "simple"
    Then it has a conclusion with id "c" and label "The system is correct"
      And it has a strategy with id "s" and label "Testing"
      And it has evidence with id "e1" and label "Test results"
      And the strategy "s" supports the conclusion "c"
      And the evidence "e1" supports the strategy "s"

  Scenario: DOT export qualifies node ids with the model name
    Given the source file "simple_justification.jd"
    When I compile it into a unit
    Then the unit contains a justification named "simple"
    When I export the current model to DOT format
    Then the DOT output contains a node with id "simple:c"
      And the DOT output contains a node with id "simple:s"
      And the DOT output contains a node with id "simple:e1"

  Scenario: elements and relations declared out of order
    Given the source file "unordered.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "j"
    Then it has a conclusion with id "c" and label "a conclusion"
      And it has a strategy with id "s" and label "a strategy"
      And it has evidence with id "e" and label "an evidence"
      And the strategy "s" supports the conclusion "c"
      And the evidence "e" supports the strategy "s"
