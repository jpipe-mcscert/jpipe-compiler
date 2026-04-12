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
    And it has a strategy with id "a_claim:s" and label "A strategy"
    And it has evidence with id "a_claim:e" and label "An evidence"
    And it has a strategy with id "another_claim:s" and label "Another strategy"
    And it has evidence with id "another_claim:e" and label "Another evidence"
    And the strategy "assembleStrategy" supports the conclusion "assembleConclusion"
    And the sub-conclusion "a_claim:c" supports the strategy "assembleStrategy"
    And the strategy "a_claim:s" supports the sub-conclusion "a_claim:c"
    And the evidence "a_claim:e" supports the strategy "a_claim:s"
    And the sub-conclusion "another_claim:c" supports the strategy "assembleStrategy"
    And the strategy "another_claim:s" supports the sub-conclusion "another_claim:c"
    And the evidence "another_claim:e" supports the strategy "another_claim:s"

  Scenario: assemble with three sources produces three demoted sub-conclusions
    Given the source file "010_assemble.jd"
    When I compile it into a unit
    Then the compilation succeeds
    And the unit contains a justification named "assembled_3"
    And it has a conclusion with id "assembleConclusion" and label "A global conclusion"
    And it has a strategy with id "assembleStrategy" and label "An aggregating strategy"
    And it has a sub-conclusion with id "a_claim:c" and label "A conclusion"
    And it has a sub-conclusion with id "another_claim:c" and label "Another conclusion"
    And it has a sub-conclusion with id "a_third_claim:c" and label "A third conclusion"
    And it has a strategy with id "a_claim:s" and label "A strategy"
    And it has evidence with id "a_claim:e" and label "An evidence"
    And it has a strategy with id "another_claim:s" and label "Another strategy"
    And it has evidence with id "another_claim:e" and label "Another evidence"
    And it has a strategy with id "a_third_claim:s" and label "A third strategy"
    And it has evidence with id "a_third_claim:e" and label "A third evidence"
    And the strategy "assembleStrategy" supports the conclusion "assembleConclusion"
    And the sub-conclusion "a_claim:c" supports the strategy "assembleStrategy"
    And the sub-conclusion "another_claim:c" supports the strategy "assembleStrategy"
    And the sub-conclusion "a_third_claim:c" supports the strategy "assembleStrategy"

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

  Scenario: incompatible operator call reports an execution error
    Given the source file "invalid/014_incompatible_operator_call.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "execution-error"
