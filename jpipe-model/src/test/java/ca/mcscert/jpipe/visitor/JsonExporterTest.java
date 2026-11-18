package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import org.json.JSONArray;
import org.json.JSONObject;
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
	void export_elementCarriesOnlyItsDocumentedFields() {
		// The undocumented "escaped" slug is gone (#187): the Python function
		// name it echoed differs whenever names collide or are reserved.
		JSONArray elements = new JSONObject(
				new JsonExporter().export(ModelFixtures.simpleJustification()))
				.getJSONArray("elements");

		assertThat(elements).isNotEmpty()
				.allSatisfy(e -> assertThat(((JSONObject) e).keySet())
						.containsExactlyInAnyOrder("type", "id", "label"));
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

	@Test
	void export_mergedElement_carriesQualifiedAliases() {
		String json = new JsonExporter()
				.export(ModelFixtures.unifiedJustification());

		assertThat(json).contains("\"aliases\"", "\"j:a:s\"", "\"j:b:s\"");
	}

	@Test
	void export_chainedAliases_carriesEveryIdAlongTheChain() {
		String json = new JsonExporter()
				.export(ModelFixtures.chainedAliasJustification());

		assertThat(json).contains("\"aliases\"", "\"j:outer:s\"",
				"\"j:outer:a:s1\"", "\"j:outer:b:s2\"");
	}

	@Test
	void export_keepsUnificationMintedIdsSoConsumersCanStillResolveThem() {
		String json = new JsonExporter()
				.export(ModelFixtures.siblingUnifiedIdJustification());

		assertThat(json).contains("\"j:outer:unified_0\"", "\"j:outer:a:s1\"");
	}

	@Test
	void export_withoutAliases_omitsAliasesKey() {
		String json = new JsonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(json).doesNotContain("\"aliases\"");
	}
}
