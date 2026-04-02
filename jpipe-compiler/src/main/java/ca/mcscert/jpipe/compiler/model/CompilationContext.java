package ca.mcscert.jpipe.compiler.model;

import ca.mcscert.jpipe.commands.ExecutedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	private final Map<String, Long> stats = new LinkedHashMap<>();
	private List<ExecutedAction> executedActions = List.of();

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

	/**
	 * Convenience: append a non-fatal ERROR diagnostic with source location.
	 */
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

	/** Record a named numeric statistic produced during compilation. */
	public void recordStat(String key, long value) {
		stats.put(key, value);
	}

	/** Unmodifiable view of all recorded statistics in insertion order. */
	public Map<String, Long> stats() {
		return Collections.unmodifiableMap(stats);
	}

	/**
	 * Record the ordered list of actions that occurred during interpretation.
	 */
	public void recordActions(List<ExecutedAction> actions) {
		this.executedActions = List.copyOf(actions);
	}

	/** Ordered list of actions that occurred during interpretation. */
	public List<ExecutedAction> executedActions() {
		return executedActions;
	}

}
