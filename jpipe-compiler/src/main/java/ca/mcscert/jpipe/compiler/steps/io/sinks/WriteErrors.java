package ca.mcscert.jpipe.compiler.steps.io.sinks;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Makes a failed write visible, whatever stream it happened on. Shared by the
 * sinks and by callers that hand them a stream wrapping {@link System#out}, so
 * that the check lives in one place.
 */
public final class WriteErrors {

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
	public static void flushAndCheck(OutputStream out) throws IOException {
		out.flush();
		if (out instanceof PrintStream printStream
				&& printStream.checkError()) {
			throw new IOException("write error");
		}
	}
}
