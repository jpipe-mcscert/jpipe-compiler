package ca.mcscert.jpipe.cli;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

/**
 * Last line of defence for an exception no command handled: a bug in the
 * compiler, or a failure outside its control.
 *
 * <p>
 * Picocli's default prints the whole stack trace and exits 1, which reads as an
 * ordinary compilation error. This handler prints one line, keeps the trace for
 * {@code --log-level DEBUG}, and exits {@link Main#EXIT_SYSTEM_ERROR}, as
 * ADR-0016 requires (#182).
 */
final class UnexpectedErrorHandler implements IExecutionExceptionHandler {

	private static final Logger logger = LogManager.getLogger();

	static final String ISSUES = "https://github.com/jpipe-mcscert/jpipe-compiler/issues";

	@Override
	public int handleExecutionException(Exception ex, CommandLine cmd,
			ParseResult parseResult) {
		cmd.getErr().println("internal error: " + ex);
		cmd.getErr().println("This is a bug in jPipe: please report it at "
				+ ISSUES + " (run with --log-level DEBUG for details).");
		cmd.getErr().flush();
		logger.debug("Unexpected exception", ex);
		return Main.EXIT_SYSTEM_ERROR;
	}
}
