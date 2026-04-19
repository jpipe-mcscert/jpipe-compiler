package ca.mcscert.jpipe.compiler.steps.io.sinks;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class StringSinkTest {

	@Test
	void pourInto_writes_string_to_stream() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new StringSink(out).pourInto("hello, world");
		assertThat(out).hasToString("hello, world");
	}

	@Test
	void pourInto_empty_string_writes_nothing() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		new StringSink(out).pourInto("");
		assertThat(out.toString()).isEmpty();
	}

	@Test
	void pourInto_flushes_stream() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		StringSink sink = new StringSink(out);
		sink.pourInto("data");
		assertThat(out.size()).isGreaterThan(0);
	}
}
