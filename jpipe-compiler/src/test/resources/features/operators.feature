Feature: Composition operators

  Scenario: refine operator creates result with merged sub-conclusion
    Given the source file "009_refine.jd"
    When I compile it into a unit
    Then the compilation succeeds
    And the unit contains a justification named "refined"
    And it has a conclusion with id "minimal:c" and label "A conclusion"
    And it has a sub-conclusion with id "hook" and label "an evidence is true"
    And it has a strategy with id "minimal:s" and label "A strategy"
    And it has a strategy with id "refinement:s" and label "A strategy"
    And it has evidence with id "refinement:e" and label "An evidence"
    And the strategy "minimal:s" supports the conclusion "minimal:c"
    And the sub-conclusion "hook" supports the strategy "minimal:s"
    And the strategy "refinement:s" supports the sub-conclusion "hook"
    And the evidence "refinement:e" supports the strategy "refinement:s"

  Scenario: assemble produces a justified conclusion over two sources
    Given the source file "010_assemble.jd"
    When I compile it into a unit
    Then the compilation succeeds
    And the unit contains a justification named "assembled_2"
    And it has a conclusion with id "assembleConclusion" and label "A global conclusion"
    And it has a strategy with id "assembleStrategy" and label "An aggregating strategy"
    And it has a sub-conclusion with id "a_claim:c" and label "A conclusion"
    And it has a sub-conclusion with id "another_claim:c" and label "Another conclusion"

  Scenario: assemble with three sources produces three demoted sub-conclusions
    Given the source file "010_assemble.jd"
    When I compile it into a unit
    Then the compilation succeeds
    And the unit contains a justification named "assembled_3"
    And it has a conclusion with id "assembleConclusion" and label "A global conclusion"

  Scenario: unknown operator name reports an execution error
    Given the source file "invalid/012_unknown_operator.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "execution-error"

  Scenario: missing hook argument reports an execution error
    Given the source file "invalid/013_missing_hook.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "execution-error"
