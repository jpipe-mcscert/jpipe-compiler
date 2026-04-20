package ca.mcscert.jpipe.compiler.steps.io.sinks;

import ca.mcscert.jpipe.compiler.model.Sink;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Pipeline sink that writes a {@link String} to an {@link OutputStream}
 * supplied at construction time. The caller decides whether that stream is a
 * file or standard output — this class has no knowledge of either.
 */
public final class StringSink implements Sink<String> {

	private final PrintStream out;

	public StringSink(OutputStream out) {
		this.out = new PrintStream(out);
	}

	@Override
	public void pourInto(String output) throws IOException {
		out.print(output);
		out.flush();
	}
}
