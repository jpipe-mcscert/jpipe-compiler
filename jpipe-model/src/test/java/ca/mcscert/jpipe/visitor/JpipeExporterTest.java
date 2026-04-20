package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Unit;
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
}
