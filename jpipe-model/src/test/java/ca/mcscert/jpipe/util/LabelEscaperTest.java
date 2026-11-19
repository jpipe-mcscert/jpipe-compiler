package ca.mcscert.jpipe.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

	@Test
	void nonLatinLettersAreKept() {
		assertThat(LabelEscaper.toMethodName("系统 正确")).isEqualTo("系统_正确");
		assertThat(LabelEscaper.toMethodName("Система работает"))
				.isEqualTo("система_работает");
		assertThat(LabelEscaper.toMethodName("हिन्दी परीक्षण"))
				.isEqualTo("हिन्दी_परीक्षण");
	}

	@ParameterizedTest
	@ValueSource(strings = {"aⸯb", "a ⸯ b", "aⸯⸯb"})
	void aLetterPythonRejectsInIdentifiersIsASeparator(String label) {
		// U+2E2F VERTICAL TILDE is a letter, but no identifier character.
		assertThat(LabelEscaper.toMethodName(label)).isEqualTo("a_b");
	}

	@Test
	void compatibilityFormsAreNormalised() {
		// Full-width letters and ligatures fold the way Python normalises
		// identifiers (NFKC).
		assertThat(LabelEscaper.toMethodName("ＴＥＳＴ ﬁle"))
				.isEqualTo("test_file");
	}

	@Test
	void lowercasingIgnoresTheDefaultLocale() {
		Locale saved = Locale.getDefault();
		Locale.setDefault(Locale.forLanguageTag("tr"));
		try {
			assertThat(LabelEscaper.toMethodName("CHECK INPUTS"))
					.isEqualTo("check_inputs");
		} finally {
			Locale.setDefault(saved);
		}
	}

	@Test
	void labelWithoutLetterOrDigitGivesUnderscore() {
		assertThat(LabelEscaper.toMethodName("!!!")).isEqualTo("_");
		assertThat(LabelEscaper.toMethodName("")).isEqualTo("_");
	}
}
