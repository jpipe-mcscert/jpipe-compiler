package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JsonExporterTest {

	@Test
	void export_justification_contains_name_and_type() {
		String json = new JsonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(json).contains("\"name\": \"j\"",
				"\"type\": \"justification\"");
	}

	@Test
	void export_justification_contains_all_element_ids() {
		String json = new JsonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(json).contains("\"j:c\"", "\"j:s\"", "\"j:e1\"");
	}

	@Test
	void export_justification_contains_relations() {
		String json = new JsonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(json).contains("\"relations\"", "\"source\"", "\"target\"");
	}

	@Test
	void export_template_contains_template_type() {
		String json = new JsonExporter().export(ModelFixtures.simpleTemplate());

		assertThat(json).contains("\"type\": \"template\"");
	}

	@Test
	void export_template_contains_abstract_support_element() {
		String json = new JsonExporter().export(ModelFixtures.simpleTemplate());

		assertThat(json).contains("\"abstract-support\"", "\"t:abs\"");
	}
}
