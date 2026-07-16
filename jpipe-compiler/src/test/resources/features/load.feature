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
      And it has a conclusion with id "t:c" and label "A conclusion"
      And it has a strategy with id "t:s" and label "A strategy"
      And it has evidence with id "t:abs" and label "A flat-loaded evidence"
      And the strategy "t:s" supports the conclusion "t:c"
      And the evidence "t:abs" supports the strategy "t:s"

  Scenario: loading a missing file reports a fatal error
    Given the source file "invalid/011_missing_load.jd"
    When I compile it into a unit
    Then the compilation fails with a fatal error
      And a fatal error mentions "does_not_exist.jd"

  Scenario: loading the same file twice under the same namespace is idempotent
    Given the source file "013_load_same_twice.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "base:t"
      And the unit contains a justification named "my_justification"

  Scenario: loading the same file twice without a namespace is idempotent
    Given the source file "014_load_flat_twice.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "t"
      And the unit contains a justification named "flat_justification"

  Scenario: diamond dependency (two files sharing a common load) compiles without duplicates
    Given the source file "017_load_diamond_root.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "shared:t"
      And the unit contains a justification named "left_justification"
      And the unit contains a justification named "right_justification"
      And the unit contains a justification named "root_justification"

  Scenario: circular load is reported as a fatal error
    Given the source file "invalid/017_load_cycle_a.jd"
    When I compile it into a unit
    Then the compilation fails with a fatal error
      And a fatal error mentions "Circular load detected"

  Scenario: flat-importing two files that declare the same model name is an error
    Given the source file "invalid/019_load_flat_collision.jd"
    When I compile it into a unit
    Then the compilation has validation errors
      And a validation error is reported for rule "execution-error"

  Scenario: a glob load expands into every matching file, imported flat
    Given the source file "020_load_glob_flat.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "alpha"
      And the unit contains a template named "beta"
      And the unit contains a justification named "alpha_justification"
      And the unit contains a justification named "beta_justification"

  Scenario: a glob load with a namespace shares that namespace across matches
    Given the source file "021_load_glob_namespace.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "lib:alpha"
      And the unit contains a template named "lib:beta"
      And the unit contains a justification named "alpha_justification"
      And the unit contains a justification named "beta_justification"

  Scenario: a recursive glob load also matches nested directories
    Given the source file "022_load_glob_recursive.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "lib:alpha"
      And the unit contains a template named "lib:beta"
      And the unit contains a template named "lib:gamma"
      And the unit contains a justification named "gamma_justification"

  Scenario: a glob load that matches no file is a fatal error
    Given the source file "invalid/020_load_glob_nomatch.jd"
    When I compile it into a unit
    Then the compilation fails with a fatal error
      And a fatal error mentions "No file matches load pattern"
