package ca.mcscert.jpipe.compiler.steps.io.sinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class StringSinkTest {

	@Test
	void pourInto_writes_string_to_stream() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new StringSink(out).pourInto("hello, world");
		assertThat(out).hasToString("hello, world");
	}

	@Test
	void pourInto_empty_string_writes_nothing() throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new StringSink(out).pourInto("");
		assertThat(out.toString()).isEmpty();
	}

	@Test
	void pourInto_encodes_the_text_as_utf8_whatever_the_default_charset()
			throws IOException {
		// Output to stdout used to follow the platform charset (#179).
		String text = "Le système est sûr — 安全";
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new StringSink(new PrintStream(out, false, StandardCharsets.US_ASCII))
				.pourInto(text);
		assertThat(out.toByteArray())
				.isEqualTo(text.getBytes(StandardCharsets.UTF_8));
	}

	@Test
	void pourInto_reports_a_failure_a_print_stream_only_records() {
		// A PrintStream never throws: a full disk used to exit 0 (#179).
		StringSink sink = new StringSink(new PrintStream(new FailingStream()));

		assertThatThrownBy(() -> sink.pourInto("lost"))
				.isInstanceOf(IOException.class).hasMessage("write error");
	}

	@Test
	void pourInto_propagates_a_failure_of_the_stream() {
		StringSink sink = new StringSink(new FailingStream());

		assertThatThrownBy(() -> sink.pourInto("lost"))
				.isInstanceOf(IOException.class)
				.hasMessage("No space left on device");
	}

	/** A stream whose every write fails, as a full disk does. */
	static final class FailingStream extends OutputStream {
		@Override
		public void write(int b) throws IOException {
			throw new IOException("No space left on device");
		}
	}
}
