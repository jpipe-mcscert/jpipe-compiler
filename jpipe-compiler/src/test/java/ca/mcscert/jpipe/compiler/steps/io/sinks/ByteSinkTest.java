package ca.mcscert.jpipe.compiler.steps.io.sinks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class ByteSinkTest {

	@Test
	void pourInto_writes_the_bytes_as_they_are() throws IOException {
		byte[] image = {(byte) 0x89, 'P', 'N', 'G', 0, (byte) 0xFF};
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		new ByteSink(out).pourInto(image);

		assertThat(out.toByteArray()).isEqualTo(image);
	}

	@Test
	void pourInto_reports_a_failure_a_print_stream_only_records() {
		ByteSink sink = new ByteSink(
				new PrintStream(new StringSinkTest.FailingStream()));
		byte[] image = {1, 2, 3};

		assertThatThrownBy(() -> sink.pourInto(image))
				.isInstanceOf(IOException.class).hasMessage("write error");
	}
}
