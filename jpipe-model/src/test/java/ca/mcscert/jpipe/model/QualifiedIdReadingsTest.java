package ca.mcscert.jpipe.model;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.QualifiedIdReadings.Reading;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class QualifiedIdReadingsTest {

	private static final Function<JustificationModel, String> UNIT = JustificationModel::getName;

	private static final Map<String, JustificationModel> MODELS = ProvenanceFixtures
			.nestedNamespaces();

	/** Each reading, the name of each of its models in angle brackets. */
	private static List<String> spelled(List<Reading> readings) {
		return readings.stream().map(r -> r.with(m -> "<" + m.getName() + ">"))
				.toList();
	}

	@ParameterizedTest(name = "in out, {0} reads {1}")
	@CsvSource({
			// a source's name is peeled off only when the rest is one of its
			// ids: b:ns holds no a:e, so b:ns:a:e is read through b (#202)
			"b:ns:a:e, <b>:<ns:a>:e", "b:k:e, <b>:<k>:e", "b:ns:e, <b:ns>:e",
			// ids no model names are left as they are
			"assembleConclusion, assembleConclusion", "b_ns:a:e, b_ns:a:e"})
	void an_id_of_a_composed_model_is_read_through_its_sources(String id,
			String expected) {
		assertThat(spelled(QualifiedIdReadings.in(MODELS.get("out"), id, UNIT)))
				.containsExactly(expected);
	}

	@ParameterizedTest(name = "in leaf, {0} reads {1}")
	@CsvSource({"base:mid:m, <base:mid>:m", "root:abs, <root>:abs",
			"root:s, <root>:s", "mid:m, mid:m"})
	void an_id_of_a_model_that_implements_a_template_is_read_through_the_chain(
			String id, String expected) {
		JustificationModel leaf = ProvenanceFixtures.templateChain();

		assertThat(spelled(QualifiedIdReadings.in(leaf, id, UNIT)))
				.containsExactly(expected);
	}

	@Test
	void an_id_two_models_can_hold_has_two_readings() {
		JustificationModel out = ProvenanceFixtures.collision();

		assertThat(spelled(QualifiedIdReadings.in(out, "b:ns:a:e", UNIT)))
				.containsExactlyInAnyOrder("<b>:<ns:a>:e", "<b:ns>:<a>:e");
	}

	@Test
	void an_argument_naming_an_element_of_the_result_is_read_through_the_sources() {
		// unifyExclude: source:id
		List<JustificationModel> sources = List.of(MODELS.get("b"),
				MODELS.get("b:ns"));

		assertThat(spelled(
				QualifiedIdReadings.inResult(sources, "b:ns:a:e", UNIT)))
				.containsExactly("<b>:<ns:a>:e");
	}

	@Test
	void an_argument_naming_an_element_of_a_source_is_read_in_the_sources_only() {
		// refine(base, ns) { hook: "ns:a:e" } names base's ns:a:e. Read in the
		// result too, it was also ns's a:e (review of #203).
		Map<String, JustificationModel> models = ProvenanceFixtures.hookScope();
		List<JustificationModel> sources = List.of(models.get("base"),
				models.get("ns"));

		assertThat(QualifiedIdReadings.inSources(sources, "ns:a:e", UNIT))
				.singleElement().satisfies(r -> {
					assertThat(r.source().getName()).isEqualTo("base");
					assertThat(spelled(List.of(r.reading())))
							.containsExactly("<ns:a>:e");
				});
	}

	@Test
	void a_short_form_of_an_id_of_a_source_reads_as_the_element_it_ends() {
		// refine accepts b:foo:a:e for base's extra:b:foo:a:e: it is read as
		// that element, whose every model is named (review of #203).
		Map<String, JustificationModel> models = ProvenanceFixtures
				.suffixChain();

		assertThat(QualifiedIdReadings.inSources(List.of(models.get("base")),
				"b:foo:a:e", UNIT)).singleElement()
				.satisfies(r -> assertThat(spelled(List.of(r.reading())))
						.containsExactly("<extra>:<b>:<foo:a>:e"));
	}

	@Test
	void model_names_are_matched_as_the_naming_function_writes_them() {
		// As a file loaded under b writes them: b:ns is ns there.
		Function<JustificationModel, String> underB = m -> m.getName()
				.startsWith("b:") ? m.getName().substring(2) : m.getName();
		JustificationModel out = MODELS.get("out");

		assertThat(QualifiedIdReadings.in(out, "ns:e", underB)).singleElement()
				.satisfies(r -> assertThat(r.with(UNIT)).isEqualTo("b:ns:e"));
	}
}
