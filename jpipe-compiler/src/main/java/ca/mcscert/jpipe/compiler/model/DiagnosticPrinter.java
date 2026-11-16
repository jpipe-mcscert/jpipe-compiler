package ca.mcscert.jpipe.compiler.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Prints the errors of a compilation as {@code file:line:column: severity:
 * [code] message} lines, the way a compiler answers on the console.
 *
 * <p>
 * They go through the {@value #LOGGER} logger, which sits outside the
 * {@code ca.mcscert.jpipe} hierarchy on purpose: those loggers carry the trace
 * of the compiler at work, which {@code --log-level} tunes, while diagnostics
 * are the result the user ran the compiler for, and must reach them whatever
 * that level is (ADR-0006, #186).
 */
final class DiagnosticPrinter {

	/** Name of the logger the diagnostics are printed on. */
	static final String LOGGER = "jpipe.diagnostics";

	private static final Logger diagnostics = LogManager.getLogger(LOGGER);

	private DiagnosticPrinter() {
	}

	/** Prints every ERROR and FATAL diagnostic {@code ctx} holds. */
	static void print(CompilationContext ctx) {
		for (Diagnostic d : ctx.diagnostics()) {
			if (d.isError()) {
				String loc = d.hasLocation()
						? d.source() + ":" + d.line() + ":" + d.column() + ": "
						: d.source() + ": ";
				String level = d.level().name().toLowerCase();
				String msg = d.hasCode()
						? "[" + d.code() + "] " + d.message()
						: d.message();
				diagnostics.error("{}{}: {}", loc, level, msg);
			}
		}
	}
}
