package ca.mcscert.jpipe.util;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(QualifiedIds.qualifyIfPlain(id, "t")).isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({"s, s", "t:s, s", "ns:t:s, s"})
	void shortId_isTheLastSegment(String id, String expected) {
		assertThat(QualifiedIds.shortId(id)).isEqualTo(expected);
	}
}
