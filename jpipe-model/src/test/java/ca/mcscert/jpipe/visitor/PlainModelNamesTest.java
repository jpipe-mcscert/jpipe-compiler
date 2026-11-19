package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.ProvenanceFixtures;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlainModelNamesTest {

	private static final Map<String, JustificationModel> MODELS = ProvenanceFixtures
			.nestedNamespaces();

	@Test
	void a_plain_name_is_kept_and_a_namespaced_one_is_flattened() {
		PlainModelNames names = PlainModelNames.of(List.of("t", "base:t"));

		assertThat(names.name("t")).isEqualTo("t");
		assertThat(names.name("base:t")).isEqualTo("base_t");
	}

	@Test
	void a_flattened_name_already_taken_gets_a_suffix() {
		PlainModelNames names = PlainModelNames
				.of(List.of("base_t", "base:t", "base_t_2"));

		assertThat(names.name("base_t")).isEqualTo("base_t");
		assertThat(names.name("base:t")).isEqualTo("base_t_3");
	}

	@ParameterizedTest(name = "in out, {0} is written {1}")
	@CsvSource({
			// b:ns:a:e is (b)(ns:a)(e): b:ns holds no a:e (#202)
			"b:ns:a:e, b:ns_a:e", "b:ns:e, b_ns:e", "b:k:e, b:k:e",
			// plain and unrelated ids are unchanged
			"assembleConclusion, assembleConclusion", "x:y:e, x:y:e"})
	void an_id_is_renamed_through_the_models_it_comes_from(String id,
			String expected) {
		PlainModelNames names = PlainModelNames.of(MODELS.keySet());

		assertThat(names.id(MODELS.get("out"), id)).isEqualTo(expected);
	}

	@Test
	void a_result_argument_is_renamed_through_the_call_sources() {
		PlainModelNames names = PlainModelNames.of(MODELS.keySet());
		List<JustificationModel> sources = List.of(MODELS.get("b"),
				MODELS.get("b:ns"));

		assertThat(names.resultArgument(sources, "b:ns:a:e"))
				.isEqualTo("b:ns_a:e");
	}

	@Test
	void a_source_argument_is_renamed_in_the_source_that_holds_it() {
		// refine(base, ns) { hook: "ns:a:e" }: base's ns:a:e, so ns_a:e, not
		// ns's a:e (review of #203).
		Map<String, JustificationModel> models = ProvenanceFixtures.hookScope();
		PlainModelNames names = PlainModelNames.of(models.keySet());
		List<JustificationModel> sources = List.of(models.get("base"),
				models.get("ns"));

		assertThat(names.sourceArgument(sources, "ns:a:e")).isEqualTo("ns_a:e");
	}

	@Test
	void a_short_form_of_a_source_id_is_written_in_full() {
		// b:foo:a:e stands for extra:b:foo:a:e; renaming foo:a in the middle
		// of the short form would leave an id that ends no element.
		Map<String, JustificationModel> models = ProvenanceFixtures
				.suffixChain();
		PlainModelNames names = PlainModelNames.of(models.keySet());

		assertThat(
				names.sourceArgument(List.of(models.get("base")), "b:foo:a:e"))
				.isEqualTo("extra:b:foo_a:e");
	}

	@Test
	void an_id_that_names_two_elements_keeps_its_first_reading() {
		// Two elements of out share the id b:ns:a:e, which compilation reports
		// as a duplicate id: the export keeps the reading through the first
		// source rather than fail.
		JustificationModel out = ProvenanceFixtures.collision();
		PlainModelNames names = PlainModelNames
				.of(List.of("ns:a", "b", "b:ns", "a", "out"));

		assertThat(names.id(out, "b:ns:a:e")).isEqualTo("b:ns_a:e");
	}
}
