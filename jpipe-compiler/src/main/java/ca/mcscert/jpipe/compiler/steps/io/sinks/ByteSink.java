package ca.mcscert.jpipe.compiler.steps.io.sinks;

import ca.mcscert.jpipe.compiler.model.Sink;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Pipeline sink that writes a {@code byte[]} to an {@link OutputStream}
 * supplied at construction time. Used for binary output formats (PNG, JPEG)
 * produced by an external renderer.
 */
public final class ByteSink implements Sink<byte[]> {

	private final OutputStream out;

	public ByteSink(OutputStream out) {
		this.out = out;
	}

	@Override
	public void pourInto(byte[] output) throws IOException {
		out.write(output);
	}
}
