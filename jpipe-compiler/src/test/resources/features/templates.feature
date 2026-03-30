Feature: Compiling template source files

  Scenario: simple template with abstract support
    Given the source file "004_template.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "t"
    Then it has a conclusion with id "c" and label "A conclusion"
      And it has a strategy with id "s" and label "A strategy"
      And it has an abstract support with id "abs" and label "An abstract support"
      And the strategy "s" supports the conclusion "c"
      And the abstract support "abs" supports the strategy "s"

  Scenario: immediate and indirect inheritance with overrides
    Given the source file "005_override.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "a_template"
      And the unit contains a justification named "immediate"
      And the unit contains a justification named "indirect"
    # check "immediate" justification
    Then the unit contains a justification named "immediate"
      And it has evidence with id "a_template:abs" and label "An immediate evidence"
      And the evidence "a_template:abs" supports the strategy "a_template:s"
    # check "indirect" justification
    Then the unit contains a justification named "indirect"
      And it has a sub-conclusion with id "a_template:abs" and label "An intermnediate sub-conclusion"
      And it has a strategy with id "s" and label "An indirect strategy"
      And it has evidence with id "e" and label "An evidence"
      And the evidence "e" supports the strategy "s"
      And the strategy "s" supports the sub-conclusion "a_template:abs"

  Scenario: multi-level inheritance and namespace-based overrides
    Given the source file "006_multi_inheritance.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a template named "root"
      And the unit contains a template named "intermediate"
      And the unit contains a justification named "leaf"
      And the unit contains a justification named "leaf_intermediate"
    # check "leaf" justification
    Then the unit contains a justification named "leaf"
      And it has evidence with id "root:abs1" and label "A leaf evidence #1"
      And it has evidence with id "root:abs2" and label "A leaf evidence #2"
      And the evidence "root:abs1" supports the strategy "root:s"
      And the evidence "root:abs2" supports the strategy "root:s"
    # check "leaf_intermediate" justification
    Then the unit contains a justification named "leaf_intermediate"
      And it has evidence with id "intermediate:abs_i" and label "A leaf support  #3"
      And it has evidence with id "root:abs2" and label "A leaf evidence #2"
      And the evidence "intermediate:abs_i" supports the strategy "intermediate:s"
      And the evidence "root:abs2" supports the strategy "root:s"
