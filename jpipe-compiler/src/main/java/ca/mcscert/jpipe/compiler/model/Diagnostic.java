package ca.mcscert.jpipe.compiler.model;

/**
 * A single diagnostic message produced during compilation.
 *
 * <p>
 * {@code line} and {@code column} carry the source location when known; both
 * are {@code 0} when no location is available (ANTLR lines are 1-based, so
 * {@code 0} is a safe sentinel).
 *
 * @param level
 *            severity of the diagnostic.
 * @param source
 *            path to the file that triggered it.
 * @param line
 *            1-based source line, or {@code 0} if unknown.
 * @param column
 *            0-based column offset, or {@code 0} if unknown.
 * @param message
 *            human-readable description.
 */
public record Diagnostic(Level level, String source, int line, int column,
		String message) {

	public enum Level {
		ERROR, FATAL
	}

	/** True if this diagnostic carries a known source location. */
	public boolean hasLocation() {
		return line > 0;
	}

	// ── without location ────────────────────────────────────────────────────

	public static Diagnostic error(String source, String message) {
		return new Diagnostic(Level.ERROR, source, 0, 0, message);
	}

	public static Diagnostic fatal(String source, String message) {
		return new Diagnostic(Level.FATAL, source, 0, 0, message);
	}

	// ── with location ────────────────────────────────────────────────────────

	public static Diagnostic error(String source, int line, int column,
			String message) {
		return new Diagnostic(Level.ERROR, source, line, column, message);
	}

	public static Diagnostic fatal(String source, int line, int column,
			String message) {
		return new Diagnostic(Level.FATAL, source, line, column, message);
	}

	/** True for ERROR and FATAL. */
	public boolean isError() {
		return level == Level.ERROR || level == Level.FATAL;
	}

	/** True only for FATAL. */
	public boolean isFatal() {
		return level == Level.FATAL;
	}

}
