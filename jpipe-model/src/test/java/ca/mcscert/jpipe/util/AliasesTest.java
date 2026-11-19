package ca.mcscert.jpipe.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AliasesTest {

	private static final Map<String, String> CHAIN = Map.of("a", "b", "b", "c",
			"x", "y", "y", "x");

	@ParameterizedTest(name = "{0} -> {1}")
	@CsvSource({"a, c", "b, c", "c, c", "free, free", "x, x", "y, y"})
	void follows_a_chain_to_its_end(String id, String end) {
		// x and y alias each other: the walk stops where it would go round
		// again, back where it started.
		assertThat(Aliases.follow(id, CHAIN::get)).isEqualTo(end);
	}
}
