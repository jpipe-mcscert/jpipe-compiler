Feature: Exporting compiled models to various formats

  Scenario: DOT export qualifies node ids with the model name
    Given the source file "simple_justification.jd"
    When I compile it into a unit
    Then the unit contains a justification named "simple"
    When I export the current model to DOT format
    Then the DOT output contains a node with id "simple:c"
      And the DOT output contains a node with id "simple:s"
      And the DOT output contains a node with id "simple:e1"

  Scenario: Python export produces snake_case methods with commented @jpipe_link
    Given the source file "simple_justification.jd"
    When I compile it into a unit
    Then the unit contains a justification named "simple"
    When I export the current model to Python format
    Then the Python output contains a method named "the_system_is_correct"
      And the Python output contains a method named "testing"
      And the Python output contains a method named "test_results"
      And the Python output has @jpipe_link for id "simple:c" commented out
      And the Python output has @jpipe_link for id "simple:s" commented out
      And the Python output has @jpipe_link for id "simple:e1" commented out
