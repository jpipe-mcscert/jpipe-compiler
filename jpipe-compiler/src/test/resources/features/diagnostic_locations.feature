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
      And the JSON report has a diagnostic with code "execution-error" at line 22
      And no JSON diagnostic message contains its own code

  Scenario: A bad override is reported at the override, and a dangling @support in its template
    Given the source file "invalid/032_override_errors.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a diagnostic with code "abstract-support-used" at line 14
      And the JSON report has a diagnostic with code "unresolved-override" at line 21
      And the JSON report has a diagnostic with code "execution-error" at line 22

  Scenario: A load that fails is reported at its load statement
    Given the source file "invalid/011_missing_load.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a "fatal" diagnostic at line 2, column 0
