package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Evidence;
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
}
