package ca.mcscert.jpipe.compiler.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Mutable context threaded through every step of a compilation pipeline.
 *
 * <p>
 * Carries the source file path (for error reporting) and a {@link Diagnostic}
 * bag that steps may append to without aborting the pipeline immediately. Use
 * {@link #hasFatalErrors()} to decide whether to stop early, and
 * {@link #diagnostics()} to inspect the full list at the end.
 */
public final class CompilationContext {

	private final String sourcePath;
	private final List<Diagnostic> diagnostics = new ArrayList<>();

	public CompilationContext(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	/** Path to the input file being compiled. */
	public String sourcePath() {
		return sourcePath;
	}

	/** Append a pre-built diagnostic. */
	public void report(Diagnostic diagnostic) {
		diagnostics.add(diagnostic);
	}

	/** Convenience: append a WARNING diagnostic. */
	public void warn(String message) {
		report(Diagnostic.warning(sourcePath, message));
	}

	/** Convenience: append a WARNING diagnostic with source location. */
	public void warn(int line, int column, String message) {
		report(Diagnostic.warning(sourcePath, line, column, message));
	}

	/** Convenience: append a non-fatal ERROR diagnostic. */
	public void error(String message) {
		report(Diagnostic.error(sourcePath, message));
	}

	/** Convenience: append a non-fatal ERROR diagnostic with source location. */
	public void error(int line, int column, String message) {
		report(Diagnostic.error(sourcePath, line, column, message));
	}

	/** Convenience: append a FATAL diagnostic. */
	public void fatal(String message) {
		report(Diagnostic.fatal(sourcePath, message));
	}

	/** Convenience: append a FATAL diagnostic with source location. */
	public void fatal(int line, int column, String message) {
		report(Diagnostic.fatal(sourcePath, line, column, message));
	}

	/** True if any ERROR or FATAL diagnostic has been reported. */
	public boolean hasErrors() {
		return diagnostics.stream().anyMatch(Diagnostic::isError);
	}

	/** True if any FATAL diagnostic has been reported. */
	public boolean hasFatalErrors() {
		return diagnostics.stream().anyMatch(Diagnostic::isFatal);
	}

	/** Unmodifiable view of all diagnostics in report order. */
	public List<Diagnostic> diagnostics() {
		return Collections.unmodifiableList(diagnostics);
	}

}
