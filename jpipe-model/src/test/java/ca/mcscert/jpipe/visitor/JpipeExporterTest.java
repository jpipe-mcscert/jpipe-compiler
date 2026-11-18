package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Composition;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class JpipeExporterTest {

	@Test
	void export_justification_contains_justification_keyword_and_name() {
		String jd = new JpipeExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(jd).contains("justification j");
	}

	@Test
	void export_justification_contains_element_declarations() {
		String jd = new JpipeExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(jd).contains("conclusion c", "strategy s", "evidence e1");
	}

	@Test
	void export_justification_contains_support_relations() {
		String jd = new JpipeExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(jd).contains("supports");
	}

	@Test
	void export_template_contains_template_keyword() {
		String jd = new JpipeExporter().export(ModelFixtures.simpleTemplate());

		assertThat(jd).contains("template t", "@support abs");
	}

	@Test
	void export_unit_contains_all_models() {
		Unit unit = new Unit("src");
		unit.add(ModelFixtures.simpleJustification());
		unit.add(ModelFixtures.simpleTemplate());

		String jd = new JpipeExporter().export(unit);

		assertThat(jd).contains("justification j", "template t");
	}

	@Test
	void a_label_holding_a_quote_or_a_backslash_is_escaped() {
		// A label written verbatim between quotes would not compile back once
		// a string ends at its first unescaped quote (#176).
		Justification j = new Justification("j");
		j.addElement(new Evidence("e", "Logs say \"OK\" in C:\\tmp"));

		String jd = new JpipeExporter().export(j);

		assertThat(jd)
				.contains("evidence e is \"Logs say \\\"OK\\\" in C:\\\\tmp\"");
	}

	@Test
	void an_implementing_model_declares_its_overrides_after_its_template() {
		// #185: the inherited elements and relations are not declared again.
		Template t = ModelFixtures.simpleTemplate();
		Justification j = implementing("j", t, "t");

		String jd = new JpipeExporter().export(j);

		assertThat(jd).isEqualToNormalizingNewlines("""
				template t {
				  conclusion c is "Conclusion"
				  strategy s is "Strategy"
				  @support abs is "Abstract step"
				  s supports c
				  abs supports s
				}

				justification j implements t {
				  evidence t:abs is "Concrete step"
				}
				""");
	}

	@Test
	void a_composed_model_is_written_as_its_call_after_its_sources() {
		Justification a = named("a");
		Justification b = named("b");
		Justification out = new Justification("out");
		out.recordComposition(new Composition(
				"assemble", List.of(a, b), Map.of("strategyLabel", "Check both",
						"conclusionLabel", "Both hold"),
				Set.of("unifyExclude")));

		String jd = new JpipeExporter().export(out);

		assertThat(jd).endsWith("""
				justification out is assemble(a, b) {
				  conclusionLabel: "Both hold"
				  strategyLabel: "Check both"
				}
				""".replace("\n", System.lineSeparator()))
				.containsOnlyOnce("justification a {")
				.containsOnlyOnce("justification b {");
	}

	@Test
	void a_call_without_argument_has_no_configuration_block() {
		Justification out = new Justification("out");
		out.recordComposition(new Composition("assemble", List.of(named("a")),
				Map.of(), Set.of()));

		assertThat(new JpipeExporter().export(out))
				.contains("justification out is assemble(a)"
						+ System.lineSeparator())
				.doesNotContain("assemble(a) {");
	}

	@Test
	void a_namespaced_model_is_inlined_under_a_plain_name() {
		Template t = ModelFixtures.simpleTemplate();
		Template namespaced = new Template("base:t");
		t.allElements().forEach(e -> {
			if (e instanceof Conclusion c) {
				namespaced.setConclusion(c);
			} else {
				namespaced.addElement(e);
			}
		});
		Justification j = implementing("j", namespaced, "base:t");
		Justification out = new Justification("out");
		out.recordComposition(new Composition("refine", List.of(j, j),
				Map.of("hook", "base:t:abs", "label", "base:t:abs"),
				Set.of("hook")));

		String jd = new JpipeExporter().export(out);

		assertThat(jd)
				.contains("template base_t {",
						"justification j implements base_t {",
						"evidence base_t:abs is \"Concrete step\"",
						// ids are renamed, free text is not
						"hook: \"base_t:abs\"", "label: \"base:t:abs\"")
				.doesNotContain("load");
	}

	/** {@code name} implementing {@code template}, its @support overridden. */
	private static Justification implementing(String name, Template template,
			String templateName) {
		Justification j = new Justification(name);
		j.inline(template, templateName);
		String abs = templateName + ":abs";
		Evidence concrete = new Evidence(abs, "Concrete step");
		j.removeElement(abs);
		j.addElement(concrete);
		((Strategy) j.findById(templateName + ":s").orElseThrow())
				.replaceSupport(concrete, concrete);
		return j;
	}

	private static Justification named(String name) {
		Justification j = new Justification(name);
		Conclusion c = new Conclusion("c", "C of " + name);
		Strategy s = new Strategy("s", "S of " + name);
		Evidence e = new Evidence("e", "E of " + name);
		j.setConclusion(c);
		j.addElement(s);
		j.addElement(e);
		s.addSupport(e);
		c.addSupport(s);
		return j;
	}
}
