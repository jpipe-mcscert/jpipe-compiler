Feature: Compiling justification source files

  Scenario: minimal standalone justification
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "minimal"
    Then it has a conclusion with id "c" and label "A conclusion"
      And it has a strategy with id "s" and label "A strategy"
      And it has evidence with id "e" and label "An evidence"
      And the strategy "s" supports the conclusion "c"
      And the evidence "e" supports the strategy "s"

  Scenario: elements and relations declared out of order
    Given the source file "001_unordered.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "j"
    Then it has a conclusion with id "c" and label "a conclusion"
      And it has a strategy with id "s" and label "a strategy"
      And it has evidence with id "e" and label "an evidence"
      And the strategy "s" supports the conclusion "c"
      And the evidence "e" supports the strategy "s"

  Scenario: justification with long labels and sub-conclusions
    Given the source file "002_long_labels.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "long_labels"
    Then it has a conclusion with id "c" and label "The system is correct and behaves as expected under all operational conditions"
      And it has a sub-conclusion with id "sc" and label "All individual components have been verified against their specifications"
      And it has a strategy with id "s" and label "Combining unit testing, integration testing and formal verification methods"
      And it has evidence with id "e1" and label "Automated test suite with full branch coverage across all critical modules"
      And it has a strategy with id "s2" and label "Aggregating component-level evidence through a structured review process"
      And the evidence "e1" supports the strategy "s"
      And the strategy "s" supports the sub-conclusion "sc"
      And the sub-conclusion "sc" supports the strategy "s2"
      And the strategy "s2" supports the conclusion "c"

  Scenario: shared evidence across multiple branches
    Given the source file "003_shared.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "shared"
    Then it has a conclusion with id "c" and label "A conclusion"
      And it has a strategy with id "s" and label "A strategy"
      And it has a sub-conclusion with id "sc1" and label "A sub-conclusion [#1]"
      And it has a strategy with id "s1" and label "A strategy       [#1]"
      And it has evidence with id "e1" and label "An evidence      [#1]"
      And it has a sub-conclusion with id "sc2" and label "A sub-conclusion [#2]"
      And it has a strategy with id "s2" and label "A strategy       [#2]"
      And it has evidence with id "e2" and label "An evidence      [#2]"
      And it has evidence with id "e" and label "A shared evidence     [#1, #2]"
      And the strategy "s" supports the conclusion "c"
      And the sub-conclusion "sc1" supports the strategy "s"
      And the strategy "s1" supports the sub-conclusion "sc1"
      And the evidence "e1" supports the strategy "s1"
      And the sub-conclusion "sc2" supports the strategy "s"
      And the strategy "s2" supports the sub-conclusion "sc2"
      And the evidence "e2" supports the strategy "s2"
      And the evidence "e" supports the strategy "s1"
      And the evidence "e" supports the strategy "s2"
