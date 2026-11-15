package ca.mcscert.jpipe.compiler.steps.io.sinks;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/** Makes the sinks report a failed write, whatever stream they were given. */
final class WriteErrors {

	private WriteErrors() {
	}

	/**
	 * Flushes {@code out}, and raises the error a {@link PrintStream} (such as
	 * {@link System#out}) records instead of throwing. Without this, a full
	 * disk or a closed pipe would end the run with exit code 0 and an empty
	 * artefact.
	 *
	 * @throws IOException
	 *             if the stream failed, now or on an earlier write.
	 */
	static void flushAndCheck(OutputStream out) throws IOException {
		out.flush();
		if (out instanceof PrintStream printStream
				&& printStream.checkError()) {
			throw new IOException("write error");
		}
	}
}
