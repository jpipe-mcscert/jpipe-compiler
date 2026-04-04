Feature: Load directive

  Scenario: load a file under a namespace alias and implement its template
    Given the source file "007_load_user.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "base:t"
      And the unit contains a justification named "my_justification"
    Then the unit contains a justification named "my_justification"
      And it has evidence with id "base:t:abs" and label "A concrete evidence overriding the abstract support"
      And the evidence "base:t:abs" supports the strategy "base:t:s"
      And the strategy "base:t:s" supports the conclusion "base:t:c"

  Scenario: load a file without a namespace alias imports symbols flat
    Given the source file "008_load_flat.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "t"
      And the unit contains a justification named "flat_justification"
    Then the unit contains a justification named "flat_justification"
      And it has evidence with id "t:abs" and label "A flat-loaded evidence"
      And the evidence "t:abs" supports the strategy "t:s"

  Scenario: loading a missing file reports a fatal error
    Given the source file "invalid/011_missing_load.jd"
    When I compile it into a unit
    Then the compilation fails with a fatal error
      And a fatal error mentions "does_not_exist.jd"
