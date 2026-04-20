Feature: Consistency and completeness validation

  Scenario: duplicate element ids within a model causes a semantic error
    Given the source file "invalid/000_duplicate_ids.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "no-duplicate-ids"

  Scenario: model without a conclusion causes a semantic error
    Given the source file "invalid/001_no_conclusion.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "conclusion-present"

  Scenario: unsupported elements cause semantic errors
    Given the source file "invalid/002_unsupported_elements.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "conclusion-supported"

  Scenario: unknown symbol in support relation causes a semantic error
    Given the source file "invalid/003_unknown_symbol.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "unknown-element"

  Scenario: duplicate model name causes a semantic error
    Given the source file "invalid/004_duplicate_models.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "execution-error"

  Scenario: multiple conclusions in a model causes a semantic error
    Given the source file "invalid/005_multiple_conclusion.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "single-conclusion"

  Scenario: un-overridden abstract support in a justification causes a semantic error
    Given the source file "invalid/006_abstract_not_overridden.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "no-abstract-support"

  Scenario: template with no abstract support causes a semantic error
    Given the source file "invalid/007_template_no_abstract.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "has-abstract-support"

  Scenario: override targeting wrong namespace or identifier causes a semantic error
    Given the source file "invalid/008_unknown_override_target.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "no-abstract-support"

  Scenario: cyclic support chain causes a semantic error
    Given the source file "invalid/009_support_cycle.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "acyclic-support"

  Scenario: mutually implementing templates causes a semantic error
    Given the source file "invalid/010_implements_cycle.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    And a validation error is reported for rule "cyclic-implements"
