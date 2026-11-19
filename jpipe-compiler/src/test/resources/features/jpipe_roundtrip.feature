Feature: The JPIPE export is a round-trip

  Exporting a model to JPIPE and compiling the result gives back the same
  model: the same elements, ids, relations and aliases. The output is
  self-contained: it holds the templates the model implements and the models
  it was composed from, never a load. A model loaded under a namespace is
  renamed to a plain name (base:t becomes base_t), and so are the ids naming it.

  Scenario Outline: Every model of <file> compiles back from its JPIPE export
    Given the source file "<file>"
    When I compile it into a unit
    Then every model of the unit compiles back from its JPIPE export

    Examples:
      | file |
      | 000_minimal.jd |
      | 001_unordered.jd |
      | 002_long_labels.jd |
      | 003_shared.jd |
      | 004_template.jd |
      | 005_override.jd |
      | 006_multi_inheritance.jd |
      | 007_load_user.jd |
      | 008_load_flat.jd |
      | 009_refine.jd |
      | 010_assemble.jd |
      | 011_unifying_while_compising.jd |
      | 012_chaining_operators.jd |
      | 013_load_same_twice.jd |
      | 014_load_flat_twice.jd |
      | 015_load_diamond_left.jd |
      | 016_load_diamond_right.jd |
      | 017_load_diamond_root.jd |
      | 020_load_glob_flat.jd |
      | 021_load_glob_namespace.jd |
      | 022_load_glob_recursive.jd |
      | 023_load_glob_relative.jd |
      | 024_assemble_commutativity.jd |
      | 025_string_literals.jd |
      | 026_declaration_order.jd |
      | 027_load_under_namespaces.jd |
      | 028_refine_templates_and_chains.jd |
