Feature: Compiler error handling

  Scenario: missing input file triggers a system error
    Given the source file "does_not_exist.jd"
    When I compile it into a unit
    Then the compilation fails with a system error
