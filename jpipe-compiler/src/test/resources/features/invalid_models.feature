Feature: Consistency and completeness validation

  # -----------------------------------------------------------------------
  # Consistency rules
  # -----------------------------------------------------------------------

  Scenario: duplicate element IDs are rejected
    Given the source file "invalid/duplicate_ids.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "no-duplicate-ids"

  Scenario: a cycle in the support graph is rejected
    Given the source file "invalid/support_cycle.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "acyclic-support"

  Scenario: a cycle in the implements chain is rejected
    Given the source file "invalid/implements_cycle.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "acyclic-implements"

  Scenario: multiple conclusions in a justification are rejected
    Given the source file "invalid/duplicate_conclusion.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "single-conclusion"

  # -----------------------------------------------------------------------
  # Completeness rules — common
  # -----------------------------------------------------------------------

  Scenario: a justification without a conclusion is rejected
    Given the source file "invalid/no_conclusion.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "conclusion-present"

  Scenario: an unsupported conclusion is rejected
    Given the source file "invalid/unsupported_conclusion.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "conclusion-supported"

  Scenario: a strategy with no supporting leaf is rejected
    Given the source file "invalid/unsupported_strategy.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "strategy-supported"

  Scenario: a sub-conclusion with no supporting strategy is rejected
    Given the source file "invalid/unsupported_sub_conclusion.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "sub-conclusion-supported"

  # -----------------------------------------------------------------------
  # Completeness rules — type-specific
  # -----------------------------------------------------------------------

  Scenario: an abstract support not overridden in a justification is rejected
    Given the source file "invalid/abstract_not_overridden.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "no-abstract-support"

  Scenario: a template with no abstract support is rejected
    Given the source file "invalid/template_no_abstract.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "has-abstract-support"
