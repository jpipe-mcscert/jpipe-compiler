package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import picocli.CommandLine;
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
 *
 * <p>
 * The output goes through an {@link OutputTarget}: a file given with {@code -o}
 * is written only once the command has produced its output, so a run that
 * aborts leaves the previous file as it was. A run that reports non-fatal
 * errors still writes its export, as ADR-0016 requires. An output that is the
 * input file is refused before anything runs (#174).
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
		if (!parent.headless && !suppressLogo()) {
			System.err.println(Logo.BANNER);
		}
		if (OutputTarget.sameFile(input, output)) {
			System.err.println("error: '" + output
					+ "' is also the input: writing to it would replace the source");
			return CommandLine.ExitCode.USAGE;
		}
		try {
			OutputTarget target = OutputTarget.open(output, System.out);
			Integer exitCode = doCall(target.stream());
			target.commit();
			return exitCode;
		} catch (CompilationException e) {
			System.err.println("error: " + e.getMessage());
			// A source that fails while being read is a system error, as one
			// that cannot be opened is (ADR-0016).
			return e.getCause() instanceof IOException
					? Main.EXIT_SYSTEM_ERROR
					: Main.EXIT_JPIPE_ERROR;
		} catch (UnsupportedOperationException e) {
			System.err.println("error: " + e.getMessage());
			return Main.EXIT_JPIPE_ERROR;
		} catch (IOException e) {
			// The output cannot be written (UnwritableOutputException), or the
			// source cannot be read under `process`: both are system errors,
			// whose message names the file (ADR-0016).
			System.err.println("error: " + e.getMessage());
			return Main.EXIT_SYSTEM_ERROR;
		}
	}

	/**
	 * Whether this invocation must not print the logo, over and above the
	 * global {@code --headless} flag.
	 *
	 * <p>
	 * Subcommands override this when their output is meant for tools, which
	 * have no use for the banner even on standard error. The default is
	 * {@code false}: the logo is shown unless {@code --headless} was passed.
	 *
	 * @return true to suppress the logo for this invocation.
	 */
	protected boolean suppressLogo() {
		return false;
	}

	/**
	 * Performs the command-specific compilation work.
	 *
	 * @param out
	 *            the resolved output stream (a buffer standing for the
	 *            {@code -o} file, or {@code System.out}). Not to be closed.
	 * @return exit code ({@link Main#EXIT_OK} or
	 *         {@link Main#EXIT_JPIPE_ERROR}).
	 * @throws IOException
	 *             any I/O failure; caught and reported by {@link #call()}.
	 */
	protected abstract Integer doCall(OutputStream out) throws IOException;
}
