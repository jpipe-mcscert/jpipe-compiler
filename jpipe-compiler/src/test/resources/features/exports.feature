Feature: Exporting compiled models to various formats

  Scenario: DOT export qualifies node ids with the model name
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the unit contains a justification named "minimal"
    When I export the current model to DOT format
    Then the DOT output contains a node with id "minimal:c"
      And the DOT output contains a node with id "minimal:s"
      And the DOT output contains a node with id "minimal:e"

  Scenario: Python export produces snake_case methods with active @jpipe_link
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the unit contains a justification named "minimal"
    When I export the current model to Python format
    Then the Python output contains a method named "a_strategy"
      And the Python output contains a method named "an_evidence"
      And the Python output has @jpipe_link for id "minimal:s" active
      And the Python output has @jpipe_link for id "minimal:e" active
      And the Python output has no @jpipe_link for id "c"

  Scenario: Python export leaves out the conclusion the runner never executes
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the unit contains a justification named "minimal"
    When I export the current model to Python format
    Then the Python output has no @jpipe_link for id "minimal:c"
      And the Python output has @jpipe_link for id "minimal:s" active
      And the Python output has @jpipe_link for id "minimal:e" active

  Scenario: JSON export preserves unification aliases on the merged element
    Given the source file "011_unifying_while_compising.jd"
    When I compile it into a unit
    Then the unit contains a justification named "assembled_2"
    When I export the current model to JSON format
    Then the JSON output contains "assembled_2:unified_0"
      And the JSON output contains "\"aliases\""
      And the JSON output contains "assembled_2:a_claim:s1"
      And the JSON output contains "assembled_2:another_claim:s2"

  Scenario: Python export names the merged element by its original ids only
    Given the source file "011_unifying_while_compising.jd"
    When I compile it into a unit
    Then the unit contains a justification named "assembled_2"
    When I export the current model to Python format
    Then the Python output has @jpipe_link for id "a_claim:s1" active
      And the Python output has @jpipe_link for id "another_claim:s2" active
      And the Python output has no @jpipe_link for id "s1"
      And the Python output has no @jpipe_link for id "assembled_2:unified_0"
      And the Python output has no @jpipe_link for id "unified_0"

  Scenario: Python export groups elements under a banner per namespace
    Given the source file "011_unifying_while_compising.jd"
    When I compile it into a unit
    Then the unit contains a justification named "assembled_2"
    When I export the current model to Python format
    Then the Python output has a namespace section for "a_claim"
      And the Python output has a namespace section for "another_claim"
      And the Python output has a namespace section for "assembled_2"

  Scenario: Python export orders a namespace bottom-up, evidence first
    Given the source file "011_unifying_while_compising.jd"
    When I compile it into a unit
    Then the unit contains a justification named "assembled_2"
    When I export the current model to Python format
    Then the Python output declares "an_evidence" before "a_strategy"
      And the Python output declares "a_strategy" before "a_conclusion"
      And the Python output declares "a_shared_evidence" before "an_aggregating_strategy"

  Scenario: Python export resolves a composition of a composition to the authored ids
    Given the source file "012_chaining_operators.jd"
    When I compile it into a unit
    Then the unit contains a justification named "with_refine_1"
    When I export the current model to Python format
    # The refine hook merges the base's evidence with the refinement's
    # conclusion into a sub-conclusion, so it is emitted commented out.
    Then the Python output has @jpipe_link for id "first:e" commented out
      And the Python output has @jpipe_link for id "second:e" commented out
      And the Python output has @jpipe_link for id "refine_1:c" commented out
      And the Python output has no @jpipe_link for id "unified_0"
