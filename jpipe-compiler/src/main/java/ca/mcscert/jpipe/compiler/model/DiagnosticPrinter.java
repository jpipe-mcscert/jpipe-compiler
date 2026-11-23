package ca.mcscert.jpipe.compiler.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.PrintWriter;
import java.util.Locale;

/**
 * Prints the errors of a compilation as {@code file:line:column: severity:
 * [code] message} lines, the way a compiler answers on the console.
 *
 * <p>
 * Diagnostics are the result the user ran the compiler for, not a trace of its
 * work, so no log level may hide them (ADR-0006, #186). They are printed either
 * on a writer the caller provides, which is how the CLI writes them straight to
 * standard error without starting a logging back end (#216), or, when the
 * caller provides none, on the {@value #LOGGER} logger. That logger sits
 * outside the {@code ca.mcscert.jpipe} hierarchy on purpose: those loggers
 * carry the trace of the compiler at work, which a log level tunes.
 */
final class DiagnosticPrinter {

	/** Name of the logger the diagnostics are printed on by default. */
	static final String LOGGER = "jpipe.diagnostics";

	private static final Logger diagnostics = LogManager.getLogger(LOGGER);

	private DiagnosticPrinter() {
	}

	/**
	 * Prints every ERROR and FATAL diagnostic {@code ctx} holds on the
	 * {@value #LOGGER} logger.
	 */
	static void print(CompilationContext ctx) {
		for (Diagnostic d : ctx.diagnostics()) {
			if (d.isError()) {
				diagnostics.error("{}", () -> line(d));
			}
		}
	}

	/**
	 * Prints every ERROR and FATAL diagnostic {@code ctx} holds on {@code out},
	 * one per line, and flushes it.
	 */
	static void print(CompilationContext ctx, PrintWriter out) {
		for (Diagnostic d : ctx.diagnostics()) {
			if (d.isError()) {
				out.println(line(d));
			}
		}
		out.flush();
	}

	/** One diagnostic as the line a compiler prints for it. */
	static String line(Diagnostic d) {
		return d.where() + ": " + d.level().name().toLowerCase(Locale.ROOT)
				+ ": " + d.codedMessage();
	}
}
