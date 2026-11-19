Feature: Every diagnostic says where, and only once

  Tools such as the IDE position a diagnostic by its file, line and column, so
  every diagnostic carries the file it is about and the line of the statement
  that caused it. One mistake gives one error: the compiler goes on after a
  failing statement, so the statements after it are not reported as missing
  (#173, #183).

  Scenario: An error in a loaded file is reported in that file
    Given the source file "invalid/029_error_in_loaded_file.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a diagnostic with code "unknown-element" in "029_loaded_with_error.jd" at line 14

  Scenario: One failing relation gives one error, not a cascade
    Given the source file "invalid/030_one_failure_no_cascade.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a diagnostic with code "invalid-support" at line 16
      And the JSON report has no diagnostic with code "strategy-supported"

  Scenario: A failing operator call is reported at the call
    Given the source file "invalid/031_operator_errors.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a diagnostic with code "unknown-model" at line 17
      And the JSON report has a diagnostic with code "invalid-operator-call" at line 22
      And the JSON report has no diagnostic at line 26
      And no JSON diagnostic message contains its own code

  Scenario: A bad override is reported at the override, and a dangling @support in its template
    Given the source file "invalid/032_override_errors.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a diagnostic with code "abstract-support-used" at line 14
      And the JSON report has a diagnostic with code "unresolved-override" at line 21
      And the JSON report has a diagnostic with code "execution-error" at line 22
      And the JSON report has no diagnostic with code "strategy-supported"

  Scenario: A source with a mistake of its own is still composed
    Given the source file "invalid/036_mistake_in_a_composed_source.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And the unit contains a justification named "r"
      And the unit contains a justification named "q"
      And the unit contains a justification named "p"
    When I produce a JSON diagnostic report
    Then the JSON report has a diagnostic with code "unknown-element" at line 16
      And the JSON report has a diagnostic with code "unknown-element" at line 52
      And the JSON report has no diagnostic with code "unresolved-symbol"
      And the JSON report has no diagnostic with code "strategy-supported"
      And the JSON report has no diagnostic with code "sub-conclusion-supported"

  Scenario: No operator composes a model whose implements never ran
    Given the source file "invalid/037_operator_on_an_unbuilt_model.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a diagnostic with code "unknown-model" at line 9
      And the JSON report has no diagnostic with code "execution-error"
      And the JSON report has no diagnostic with code "unresolved-symbol"

  Scenario: A model declared again in another file is reported, and leaves the first untouched
    Given the source file "invalid/033_duplicate_across_files.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "duplicate-model"
      And the unit contains a template named "t"
    Then it has a conclusion with id "c" and label "A conclusion"
    When I produce a JSON diagnostic report
    Then the JSON report has a diagnostic with code "duplicate-model" at line 12
      And the JSON report has no diagnostic with code "execution-error"

  Scenario: A load that fails is reported at its load statement
    Given the source file "invalid/011_missing_load.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a "fatal" diagnostic at line 2, column 0
