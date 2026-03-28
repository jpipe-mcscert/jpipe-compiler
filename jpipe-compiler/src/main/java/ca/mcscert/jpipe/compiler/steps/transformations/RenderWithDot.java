package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Compilation step that renders DOT text to a binary image format by invoking
 * the {@code dot} command-line tool.
 *
 * <p>
 * The {@code dot} binary must be available on {@code PATH}. Use
 * {@code jpipe --doctor} to verify before running.
 *
 * <p>
 * stdin is written in a virtual thread while stdout is read in the caller
 * thread, avoiding deadlock when the process's pipe buffers are smaller than
 * the input.
 */
public class RenderWithDot extends Transformation<String, byte[]> {

	private final String dotFormat;

	/**
	 * @param dotFormat
	 *            the {@code dot -T} format name, e.g. {@code "png"} or
	 *            {@code "jpeg"}.
	 */
	public RenderWithDot(String dotFormat) {
		this.dotFormat = dotFormat;
	}

	@Override
	protected byte[] run(String dotSource, CompilationContext ctx) throws Exception {
		Process process;
		try {
			process = new ProcessBuilder("dot", "-T" + dotFormat).start();
		} catch (IOException e) {
			throw new IOException("dot not found on PATH — install Graphviz or run 'jpipe --doctor'", e);
		}

		Thread writer = Thread.ofVirtual().start(() -> {
			try (OutputStream stdin = process.getOutputStream()) {
				stdin.write(dotSource.getBytes(StandardCharsets.UTF_8));
			} catch (IOException ignored) {
			}
		});

		byte[] result = process.getInputStream().readAllBytes();
		writer.join();
		int exitCode = process.waitFor();

		if (exitCode != 0) {
			String error = new String(process.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
			throw new IOException("dot exited with code " + exitCode + ": " + error.strip());
		}
		return result;
	}
}
