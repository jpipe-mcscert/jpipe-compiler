package ca.mcscert.jpipe.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class QualifiedIdsTest {

	@Test
	void qualify_prefixesTheIdWithAColon() {
		assertThat(QualifiedIds.qualify("model", "s")).isEqualTo("model:s");
	}

	@ParameterizedTest
	@CsvSource({"s, t:s", "u:s, u:s", "ns:u:s, ns:u:s"})
	void qualifyIfPlain_qualifiesOnlyAPlainId(String id, String expected) {
		assertThat(QualifiedIds.qualifyIfPlain("t", id)).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({"s, s", "t:s, s", "ns:t:s, s"})
	void shortId_isTheLastSegment(String id, String expected) {
		assertThat(QualifiedIds.shortId(id)).isEqualTo(expected);
	}

	@ParameterizedTest(name = "{0} is {1}")
	@CsvSource({"e, e", "a:e, A:e", "a:b:e, AB:e", "b:a:e, b:A:e",
			"a:a:e, A:A:e", "x:e, x:e", "a:b, A:b"})
	void renameModels_replacesTheLongestRunThatNamesAModel(String id,
			String expected) {
		// a and a:b are models; the last segment is never part of a run.
		Map<String, String> models = Map.of("a", "A", "a:b", "AB");

		assertThat(QualifiedIds.renameModels(id,
				run -> Optional.ofNullable(models.get(run))))
				.isEqualTo(expected);
	}
}
