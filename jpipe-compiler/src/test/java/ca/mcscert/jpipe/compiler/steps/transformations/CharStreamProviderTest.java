package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.compiler.model.Diagnostic;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.misc.Interval;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class CharStreamProviderTest {

	@Test
	void decodes_utf8_text() {
		String text = "conclusion c is \"Le système est sûr — 安全\"\n";
		CompilationContext ctx = new CompilationContext("f.jd");

		CharStream stream = new CharStreamProvider()
				.fire(stream(text.getBytes(StandardCharsets.UTF_8)), ctx);

		assertThat(stream.getText(Interval.of(0, stream.size() - 1)))
				.isEqualTo(text);
		assertThat(ctx.diagnostics()).isEmpty();
	}

	@Test
	void a_source_that_fails_while_read_is_a_recorded_fatal() {
		// The fatal must be in the context, not only in the exception:
		// `diagnostic` reports from the context (#180).
		InputStream failing = new InputStream() {
			@Override
			public int read() throws IOException {
				throw new IOException("Is a directory");
			}
		};
		CompilationContext ctx = new CompilationContext("<stdin>");
		CharStreamProvider step = new CharStreamProvider();

		assertThatThrownBy(() -> step.fire(failing, ctx))
				.isInstanceOf(CompilationException.class)
				.hasMessageContaining("cannot read source: Is a directory");
		assertThat(ctx.hasFatalErrors()).isTrue();
	}

	static Stream<Arguments> invalidSources() {
		byte[] latin1E = {(byte) 0xE9};
		return Stream.of(
				// A Latin-1 é, on the second line, after 5 characters.
				Arguments.of("a latin-1 letter",
						concat(utf8("line 1\n\"Syst"), latin1E, utf8("me\"")),
						2, 5, "E9"),
				// The column counts code points, as the lexer does: 安 is one.
				Arguments.of("after a wide character",
						concat(utf8("\"安 "), latin1E, utf8("\"")), 1, 3, "E9"),
				// A multi-byte sequence cut short by the end of the file.
				Arguments.of("a truncated sequence",
						concat(utf8("\"x"), new byte[]{(byte) 0xC3}), 1, 2,
						"C3"));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("invalidSources")
	void an_invalid_byte_is_a_fatal_located_where_it_sits(String name,
			byte[] source, int line, int column, String hex) {
		// Invalid bytes used to be replaced with U+FFFD silently (#179).
		CompilationContext ctx = new CompilationContext("f.jd");
		CharStreamProvider step = new CharStreamProvider();
		InputStream input = stream(source);

		assertThatThrownBy(() -> step.fire(input, ctx))
				.isInstanceOf(CompilationException.class)
				.hasMessageContaining("invalid UTF-8 byte 0x" + hex);
		assertThat(ctx.hasFatalErrors()).isTrue();
		Diagnostic fatal = ctx.diagnostics().getFirst();
		assertThat(fatal.line()).isEqualTo(line);
		assertThat(fatal.column()).isEqualTo(column);
	}

	private static InputStream stream(byte[] bytes) {
		return new ByteArrayInputStream(bytes);
	}

	private static byte[] utf8(String text) {
		return text.getBytes(StandardCharsets.UTF_8);
	}

	private static byte[] concat(byte[]... parts) {
		int length = 0;
		for (byte[] part : parts) {
			length += part.length;
		}
		byte[] all = new byte[length];
		int at = 0;
		for (byte[] part : parts) {
			System.arraycopy(part, 0, all, at, part.length);
			at += part.length;
		}
		return all;
	}
}
