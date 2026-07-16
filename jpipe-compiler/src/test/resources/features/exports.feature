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
    Then the Python output contains a method named "a_conclusion"
      And the Python output contains a method named "a_strategy"
      And the Python output contains a method named "an_evidence"
      And the Python output has @jpipe_link for id "minimal:c" active
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

  Scenario: Python export links the merged element by every original id
    Given the source file "011_unifying_while_compising.jd"
    When I compile it into a unit
    Then the unit contains a justification named "assembled_2"
    When I export the current model to Python format
    Then the Python output has @jpipe_link for id "assembled_2:unified_0" active
      And the Python output has @jpipe_link for id "assembled_2:a_claim:s1" active
      And the Python output has @jpipe_link for id "assembled_2:another_claim:s2" active
