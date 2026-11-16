package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class StringLiteralTest {

	@ParameterizedTest(name = "{0} denotes {1}")
	@CsvSource(delimiter = '|', quoteCharacter = '`', value = {
			"`\"A label\"`               | `A label`",
			"`\"\"`                      | ``",
			"`\"Logs say \\\"OK\\\"\"`   | `Logs say \"OK\"`",
			"`\"ends with \\\\\"`        | `ends with \\`",
			"`\"C:\\dir\\file.jd\"`      | `C:\\dir\\file.jd`",
			"`\"a \\\\\\\" b\"`          | `a \\\" b`"})
	void a_token_denotes_its_text_with_escapes_resolved(String token,
			String value) {
		assertThat(StringLiteral.value(token)).isEqualTo(value);
	}

	@ParameterizedTest(name = "{0} is no string literal")
	@ValueSource(strings = {"", "\"", "abc", "'abc'"})
	void anything_but_a_double_quoted_token_is_refused(String token) {
		assertThatThrownBy(() -> StringLiteral.value(token))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("double-quoted");
	}
}
