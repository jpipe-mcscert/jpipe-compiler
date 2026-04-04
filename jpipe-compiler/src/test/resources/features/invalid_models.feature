Feature: Consistency and completeness validation

  Scenario: unknown symbol in support relation causes a semantic error
    Given the source file "invalid/003_unknown_symbol.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "unknown-element"

  Scenario: duplicate model name causes a system error
    Given the source file "invalid/004_duplicate_models.jd"
    When I compile it into a unit
    Then the compilation fails with a system error

  Scenario: override targeting wrong namespace or identifier causes a system error
    Given the source file "invalid/007_unknown_override_target.jd"
    When I compile it into a unit
    Then the compilation fails with a system error
