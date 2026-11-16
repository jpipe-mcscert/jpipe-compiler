package ca.mcscert.jpipe.compiler.steps.io.sinks;

import ca.mcscert.jpipe.compiler.model.Sink;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Pipeline sink that writes a {@link String} to an {@link OutputStream}
 * supplied at construction time. The caller decides whether that stream is a
 * file or standard output — this class has no knowledge of either.
 *
 * <p>
 * The text is always encoded as UTF-8, the encoding sources are read in, so an
 * export reads the same on standard output as in a file, whatever the
 * platform's default charset. A failed write is raised as an
 * {@link IOException}, including on a {@link java.io.PrintStream}, which only
 * records it (#179).
 */
public final class StringSink implements Sink<String> {

	private final OutputStream out;

	public StringSink(OutputStream out) {
		this.out = out;
	}

	@Override
	public void pourInto(String output) throws IOException {
		out.write(output.getBytes(StandardCharsets.UTF_8));
		WriteErrors.flushAndCheck(out);
	}
}
