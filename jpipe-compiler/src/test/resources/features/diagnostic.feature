Feature: Reporting on a compilation unit

  The diagnostic report describes a compilation without exporting any model.
  It is rendered either as human-readable text or as JSON for tooling; both
  formats are built from the same collected snapshot, so they always describe
  the same compilation.

  Scenario: The text report describes a clean compilation section by section
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the compilation succeeds
    When I produce a text diagnostic report
    Then the text report contains "=== Diagnostics ==="
      And the text report contains "(none)"
      And the text report contains "=== Model Summary ==="
      And the text report contains "justification \"minimal\""
      And the text report contains "=== Symbol Table ==="
      And the text report contains "=== Executed Actions ==="

  Scenario: The JSON report describes a clean compilation
    Given the source file "000_minimal.jd"
    When I compile it into a unit
    Then the compilation succeeds
    When I produce a JSON diagnostic report
    Then the JSON report declares a schema version
      And the JSON report status is "ok"
      And the JSON report describes a "justification" named "minimal"

  Scenario: The JSON report exposes validation rules as machine-readable codes
    Given the source file "invalid/000_duplicate_ids.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    When I produce a JSON diagnostic report
    Then the JSON report status is "errors"
      And the JSON report has a diagnostic with code "no-duplicate-ids"
      And no JSON diagnostic message contains its own code

  Scenario: Compiler diagnostics carry their code as data too
    Given the source file "invalid/003_unknown_symbol.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    When I produce a JSON diagnostic report
    Then the JSON report has a diagnostic with code "unknown-element"
      And no JSON diagnostic message contains its own code

  Scenario: Both renderings describe the same compilation
    Given the source file "invalid/002_unsupported_elements.jd"
    When I compile it into a unit
    Then the compilation has validation errors
    When I produce a text diagnostic report
      And I produce a JSON diagnostic report
    Then both reports agree on the number of diagnostics
      And the text report contains "[conclusion-supported]"
      And the JSON report has a diagnostic with code "conclusion-supported"

  Scenario: A template and its implementors appear in the JSON report
    Given the source file "004_template.jd"
    When I compile it into a unit
    Then the compilation succeeds
    When I produce a JSON diagnostic report
    Then the JSON report describes a "template" named "t"
      And the JSON report status is "ok"
