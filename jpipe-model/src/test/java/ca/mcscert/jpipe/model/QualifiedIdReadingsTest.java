package ca.mcscert.jpipe.model;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.QualifiedIdReadings.Reading;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class QualifiedIdReadingsTest {

	private static final Function<JustificationModel, String> UNIT = JustificationModel::getName;

	private static final Map<String, JustificationModel> MODELS = ProvenanceFixtures
			.nestedNamespaces();

	/** Each reading, the name of each of its models in angle brackets. */
	private static List<String> spelled(List<Reading> readings) {
		return readings.stream().map(r -> r.with(m -> "<" + m.getName() + ">"))
				.toList();
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
		List<JustificationModel> sources = List.of(MODELS.get("b"),
				MODELS.get("b:ns"));

		assertThat(QualifiedIdReadings.inResult(sources, "ns:e", underB))
				.singleElement()
				.satisfies(r -> assertThat(r.with(UNIT)).isEqualTo("b:ns:e"));
	}
}
