package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Composition;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.ProvenanceFixtures;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

	/** The origins of the element {@code id} of {@code json}, as text. */
	private static List<String> originsOf(String json, String id) {
		List<String> origins = new ArrayList<>();
		new JSONObject(json).getJSONArray("elements").forEach(e -> {
			JSONObject element = (JSONObject) e;
			if (element.getString("id").equals(id) && element.has("origins")) {
				element.getJSONArray("origins").forEach(o -> {
					JSONObject origin = (JSONObject) o;
					origins.add(origin.getString("model") + " "
							+ origin.getString("id") + " "
							+ origin.getString("type"));
				});
			}
		});
		return origins;
	}

	@Test
	void export_composedElementListsItsOriginsAtEveryLevelNearestFirst() {
		// out assembles b, which assembles ns:a: out's b:ns:a:e is b's ns:a:e,
		// which is the evidence e of ns:a (#200).
		JustificationModel out = ProvenanceFixtures.nestedNamespaces()
				.get("out");

		String json = new JsonExporter().export(out);

		assertThat(originsOf(json, "out:b:ns:a:e"))
				.containsExactly("b b:ns:a:e evidence", "ns:a ns:a:e evidence");
		assertThat(originsOf(json, "out:b:ns:e"))
				.containsExactly("b:ns b:ns:e evidence");
		assertThat(originsOf(json, "out:assembleConclusion")).isEmpty();
	}

	@Test
	void export_mergedElementListsEveryElementMergedIntoIt() {
		// refine merges the hook evidence and the refinement's conclusion into
		// one sub-conclusion: the aliases say which ids, the origins what
		// they were.
		Justification base = new Justification("base");
		base.addElement(new Evidence("e", "The tests pass"));
		Justification refinement = new Justification("refinement");
		refinement.setConclusion(new Conclusion("c", "The code is tested"));
		Justification refined = new Justification("refined");
		refined.addElement(new SubConclusion("hook", "The code is tested"));
		refined.recordComposition(new Composition("refine",
				List.of(base, refinement), Map.of(), Set.of()));
		refined.recordAlias("base:e", "hook");
		refined.recordAlias("refinement:c", "hook");

		String json = new JsonExporter().export(refined);

		assertThat(originsOf(json, "refined:hook")).containsExactly(
				"base base:e evidence", "refinement refinement:c conclusion");
	}

	@Test
	void export_modelNoOperatorBuiltHasNoOrigins() {
		String json = new JsonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(json).doesNotContain("\"origins\"");
	}

	@Test
	void export_implementingModelLaysOutItsTemplatesParentFirst() {
		// leaf implements base:mid, which implements root and fills its
		// @support abs (#199). A cluster draws the template's own elements,
		// then the @supports it fills.
		JSONObject layout = new JSONObject(
				new JsonExporter().export(ProvenanceFixtures.templateChain()))
				.getJSONObject("implements");

		assertThat(layout.getJSONArray("elements")).isEmpty();
		JSONArray templates = layout.getJSONArray("templates");
		assertThat(templates.getJSONObject(0).getString("name"))
				.isEqualTo("base:mid");
		assertThat(templates.getJSONObject(0).getJSONArray("elements"))
				.containsExactly("leaf:base:mid:m", "leaf:root:abs");
		JSONObject root = templates.getJSONObject(1);
		assertThat(root.getString("name")).isEqualTo("root");
		assertThat(root.getJSONArray("elements")).containsExactly("leaf:root:c",
				"leaf:root:s", "leaf:root:abs");
		JSONObject placeholder = root.getJSONArray("placeholders")
				.getJSONObject(0);
		assertThat(placeholder.getString("id")).isEqualTo("leaf:root:abs");
		assertThat(placeholder.getString("label")).isEqualTo("Some support");
		assertThat(placeholder.getJSONArray("supports"))
				.containsExactly("leaf:root:s");
	}

	@Test
	void export_placeholderIsNeverAnElement() {
		// The runner rejects an element of an unknown type: the placeholder
		// lives in the layout only.
		JSONArray elements = new JSONObject(
				new JsonExporter().export(ProvenanceFixtures.templateChain()))
				.getJSONArray("elements");

		assertThat(elements)
				.noneSatisfy(e -> assertThat(((JSONObject) e).getString("type"))
						.isEqualTo("abstract-support"));
	}

	@Test
	void export_modelImplementingNothingHasNoLayout() {
		String json = new JsonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(json).doesNotContain("\"implements\"");
	}
}
