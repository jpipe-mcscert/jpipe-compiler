package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
}
