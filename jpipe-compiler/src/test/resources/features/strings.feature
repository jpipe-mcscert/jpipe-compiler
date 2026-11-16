Feature: Lexing string literals and unrecognised characters

  A string is written between double quotes and ends at its first unescaped
  one; \" and \\ are its only escapes. A character the language does not know
  is a syntax error, located where it sits, never silently dropped (#172, #176).

  Scenario: Several strings on one line, escapes and a commented quote
    Given the source file "025_string_literals.jd"
    When I compile it into a unit
    Then the compilation succeeds
      And the unit contains a justification named "strings"
    Then it has a conclusion with id "c" and label "The release is safe"
      And it has a strategy with id "s" and label "Check the logs"
      And it has evidence with id "e" and label 'Logs say "OK"'
      And it has evidence with id "p" and label 'Archived in share\ and in C:\releases'
      And the evidence "p" supports the strategy "s"

  Scenario: A single-quoted label is a located syntax error, not a crash
    Given the source file "invalid/027_single_quotes.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a "error" diagnostic at line 10, column 20
      And the JSON report has a diagnostic with severity "fatal"
      And the JSON report describes no model

  Scenario: An unrecognised character is reported, not dropped from the id
    Given the source file "invalid/028_unrecognised_characters.jd"
    When I run the diagnostic compiler with format "json"
    Then the report signals errors
      And the JSON report has a "error" diagnostic at line 13, column 16
      And the JSON report has a diagnostic with severity "fatal"
      And the JSON report describes no model
