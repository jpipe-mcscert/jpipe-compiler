Feature: Exporting compiled models to various formats

  Scenario: DOT export qualifies node ids with the model name
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the unit contains a justification named "minimal"
    When I export the current model to DOT format
    Then the DOT output contains a node with id "minimal:c"
      And the DOT output contains a node with id "minimal:s"
      And the DOT output contains a node with id "minimal:e"

  Scenario: Python export produces snake_case steps bound by their ids
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the unit contains a justification named "minimal"
    When I export the current model to Python format
    Then the Python output contains a method named "a_strategy"
      And the Python output contains a method named "an_evidence"
      And the Python output binds id "minimal:s" on an active step
      And the Python output binds id "minimal:e" on an active step
      And the Python output binds nothing to id "c"

  Scenario: Python export comments out the conclusion the runner concludes unbound
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the unit contains a justification named "minimal"
    When I export the current model to Python format
    Then the Python output binds id "minimal:c" on a commented-out step
      And the Python output declares "a_strategy" before "a_conclusion"
      And the Python output binds id "minimal:s" on an active step
      And the Python output binds id "minimal:e" on an active step

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
    Then the Python output binds id "a_claim:s1" on an active step
      And the Python output binds id "another_claim:s2" on an active step
      And the Python output binds nothing to id "s1"
      And the Python output binds nothing to id "assembled_2:unified_0"
      And the Python output binds nothing to id "unified_0"

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
    Then the Python output binds id "first:e" on a commented-out step
      And the Python output binds id "second:e" on a commented-out step
      And the Python output binds id "refine_1:c" on a commented-out step
      And the Python output binds nothing to id "unified_0"

  Scenario: Python export qualifies the functions of elements sharing a label
    Given the source file "009_refine.jd"
    When I compile it into a unit
    Then the unit contains a justification named "expected"
    When I export the current model to Python format
    # Both strategies read "A strategy", in the same namespace, so each is
    # qualified by the id it is bound by; a lone `a_strategy` would hold one.
    Then the Python output contains a method named "expected_minimal_s_a_strategy"
      And the Python output contains a method named "expected_refinement_s_a_strategy"
      And the Python output has no method named "a_strategy"
      And the Python output contains a method named "an_evidence"
