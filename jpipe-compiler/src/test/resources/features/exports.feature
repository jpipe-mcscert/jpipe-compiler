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

  Scenario: JSON export gives refine's hook the kinds it was merged from
    Given the source file "009_refine.jd"
    When I compile it into a unit
    Then the unit contains a justification named "refined"
    When I export the current model to JSON format
    Then the JSON element "refined:hook" has the origin "minimal:e" of model "minimal", a "evidence"
      And the JSON element "refined:hook" has the origin "refinement:c" of model "refinement", a "conclusion"

  Scenario: JSON export says which conclusions assemble turned into sub-conclusions
    Given the source file "010_assemble.jd"
    When I compile it into a unit
    Then the unit contains a justification named "assembled_2"
    When I export the current model to JSON format
    Then the JSON element "assembled_2:a_claim:c" has the origin "a_claim:c" of model "a_claim", a "conclusion"
      And the JSON element "assembled_2:another_claim:c" has the origin "another_claim:c" of model "another_claim", a "conclusion"

  Scenario: JSON export gives the origins of every level of a chained composition
    Given the source file "012_chaining_operators.jd"
    When I compile it into a unit
    Then the unit contains a justification named "with_refine_1"
    When I export the current model to JSON format
    Then the JSON element "with_refine_1:hook" has the origin "both:unified_0" of model "both", a "evidence"
      And the JSON element "with_refine_1:hook" has the origin "first:e" of model "first", a "evidence"
      And the JSON element "with_refine_1:both:first:c" has the origin "first:c" of model "first", a "conclusion"

  Scenario: JSON export gives no origins to a model nothing was composed into
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the unit contains a justification named "minimal"
    When I export the current model to JSON format
    Then no JSON element has origins

  Scenario Outline: JSON origins of <file> trace every element back to its sources
    Given the source file "<file>"
    When I compile it into a unit
    Then every composed model of the unit traces each source element to one of its elements
      And every origin in the unit is an element of its model, of the kind it gives

    Examples:
      | file |
      | 009_refine.jd |
      | 010_assemble.jd |
      | 011_unifying_while_compising.jd |
      | 012_chaining_operators.jd |
      | 024_assemble_commutativity.jd |
      | 027_load_under_namespaces.jd |
      | 028_refine_templates_and_chains.jd |
      | 029_namespace_prefix_of_another.jd |

  Scenario: JSON export lays out the templates a justification implements
    Given the source file "006_multi_inheritance.jd"
    When I compile it into a unit
    Then the unit contains a justification named "leaf_intermediate"
    When I export the current model to JSON format
    Then the JSON output contains "\"implements\""
      And the JSON output contains "\"placeholders\""
      And the JSON output contains "A root abstract support #1"

  Scenario: JSON export of a model that implements nothing has no layout
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the unit contains a justification named "minimal"
    When I export the current model to JSON format
    Then the JSON output does not contain "\"implements\""

  Scenario Outline: The DOT export of every model of <file> is rebuilt from its JSON export
    Given the source file "<file>"
    When I compile it into a unit
    Then the DOT export of every model of the unit is rebuilt from its JSON export

    Examples:
      | file |
      | 000_minimal.jd |
      | 003_shared.jd |
      | 004_template.jd |
      | 005_override.jd |
      | 006_multi_inheritance.jd |
      | 007_load_user.jd |
      | 009_refine.jd |
      | 012_chaining_operators.jd |
      | 015_load_diamond_left.jd |
      | 026_declaration_order.jd |
      | 027_load_under_namespaces.jd |
      | 028_refine_templates_and_chains.jd |

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
