package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Shared scaffold for commands that read a {@code .jd} source file and write
 * output to a file or standard output.
 *
 * <p>
 * Subclasses implement {@link #doCall(OutputStream)} to perform the actual
 * compilation work; this class handles logo display, stream resolution,
 * resource management, and error reporting.
 */
abstract class InputOutputCommand implements Callable<Integer> {

	@ParentCommand
	private Main parent;

	@Option(names = {"-i",
			"--input"}, description = "Input .jd source file (default: stdin).", defaultValue = CompilationConfig.STDIN)
	protected String input;

	@Option(names = {"-o",
			"--output"}, description = "Output file (default: stdout).", defaultValue = CompilationConfig.STDOUT)
	protected String output;

	@Override
	@SuppressWarnings("java:S106") // intentional: output stream and CLI error
									// messages target stdout/stderr
	public final Integer call() {
		if (!parent.headless) {
			Logo.sout();
		}
		try (FileOutputStream fileOut = output.equals(CompilationConfig.STDOUT)
				? null
				: new FileOutputStream(output)) {
			OutputStream out = fileOut != null ? fileOut : System.out;
			return doCall(out);
		} catch (CompilationException | UnsupportedOperationException e) {
			System.err.println("error: " + e.getMessage());
			return Main.EXIT_JPIPE_ERROR;
		} catch (Exception e) {
			System.err.println("unexpected error: " + e.getMessage());
			return Main.EXIT_SYSTEM_ERROR;
		}
	}

	/**
	 * Performs the command-specific compilation work.
	 *
	 * @param out
	 *            the resolved output stream (either a file or
	 *            {@code System.out}).
	 * @return exit code ({@link Main#EXIT_OK} or
	 *         {@link Main#EXIT_JPIPE_ERROR}).
	 * @throws Exception
	 *             any compilation or I/O failure; caught and reported by
	 *             {@link #call()}.
	 */
	@SuppressWarnings("java:S112") // subclasses may throw checked exceptions
									// beyond IOException
	protected abstract Integer doCall(OutputStream out) throws Exception;
}
