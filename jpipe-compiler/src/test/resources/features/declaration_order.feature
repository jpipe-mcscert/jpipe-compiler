Feature: The order of declarations does not change what a model means

  A command waits until what it depends on is complete: an implements until
  its template is whole, a relation until every element of its model is
  declared, an operator call until its source models are complete (#177).

  Scenario: A template is copied whole, even when used before it is declared
    Given the source file "026_declaration_order.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "user"
      And the strategy "ordered:s" supports the conclusion "ordered:c"
      And the evidence "ordered:abs" supports the strategy "ordered:s"

  Scenario: A chain of templates declared bottom-up is inherited in full
    Given the source file "026_declaration_order.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "leaf"
      And the strategy "mid:s_m" supports the sub-conclusion "root:abs_r"
      And the evidence "mid:abs_m" supports the strategy "mid:s_m"
      And the evidence "root:abs_l" supports the strategy "root:s"

  Scenario: A relation binds to the elements the model declares, wherever it is written
    Given the source file "026_declaration_order.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "relations"
      And the evidence "local" supports the strategy "s2"
      And the strategy "s2" supports the sub-conclusion "two_supports:abs1"

  Scenario: A model that implements a template cannot declare its own conclusion, in either order
    Given the source file "invalid/034_own_conclusion_when_implementing.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a diagnostic with code "inherited-conclusion" at line 21
      And the JSON report has a diagnostic with code "inherited-conclusion" at line 22
      And the JSON report has a diagnostic with code "inherited-conclusion" at line 39
      And the JSON report has no diagnostic with code "unknown-element"

  Scenario: A short id that names several inherited elements is an error
    Given the source file "invalid/035_ambiguous_short_id.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a diagnostic with code "ambiguous-id" at line 28
