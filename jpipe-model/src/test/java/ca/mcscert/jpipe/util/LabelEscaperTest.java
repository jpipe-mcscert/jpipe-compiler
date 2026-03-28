package ca.mcscert.jpipe.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LabelEscaperTest {

	@Test
	void plainWords() {
		assertThat(LabelEscaper.toMethodName("The system is correct"))
				.isEqualTo("the_system_is_correct");
		assertThat(LabelEscaper.toMethodName("Testing")).isEqualTo("testing");
		assertThat(LabelEscaper.toMethodName("Test results"))
				.isEqualTo("test_results");
	}

	@Test
	void mathematicalOperators() {
		assertThat(LabelEscaper.toMethodName("Accuracy <= 0.85"))
				.isEqualTo("accuracy_0_85");
		assertThat(LabelEscaper.toMethodName("Coverage >= 90%"))
				.isEqualTo("coverage_90");
		assertThat(LabelEscaper.toMethodName("Error != 0"))
				.isEqualTo("error_0");
	}

	@Test
	void unicodeAccents() {
		assertThat(LabelEscaper.toMethodName("café test"))
				.isEqualTo("cafe_test");
		assertThat(LabelEscaper.toMethodName("Vérification"))
				.isEqualTo("verification");
		assertThat(LabelEscaper.toMethodName("naïve approach"))
				.isEqualTo("naive_approach");
	}

	@Test
	void leadingDigit() {
		assertThat(LabelEscaper.toMethodName("100% accurate"))
				.isEqualTo("_100_accurate");
		assertThat(LabelEscaper.toMethodName("42 things"))
				.isEqualTo("_42_things");
	}

	@Test
	void leadingAndTrailingSpecialChars() {
		assertThat(LabelEscaper.toMethodName("  leading/trailing  "))
				.isEqualTo("leading_trailing");
	}

	@Test
	void multipleConsecutiveSpecialChars() {
		assertThat(LabelEscaper.toMethodName("a --- b")).isEqualTo("a_b");
	}
}
