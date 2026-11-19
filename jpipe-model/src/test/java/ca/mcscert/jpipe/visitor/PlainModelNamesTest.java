package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlainModelNamesTest {

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

	@ParameterizedTest
	@CsvSource({
			// every run naming a renamed model, longest first
			"ns:both:ns:a:e, ns_both:ns_a:e", "ns:both:x:y:e, ns_both:x_y:e",
			"ns:both:x:e, ns_both:x:e",
			// the local id is never a model name
			"j:ns:both, j:ns:both",
			// plain and unrelated ids are unchanged
			"e, e", "a:e, a:e"})
	void ids_naming_a_renamed_model_are_renamed(String id, String expected) {
		PlainModelNames names = PlainModelNames
				.of(List.of("ns:both", "ns:a", "x:y", "x:y:z", "a"));

		assertThat(names.id(id)).isEqualTo(expected);
	}
}
