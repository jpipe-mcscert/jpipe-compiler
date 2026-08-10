package ca.mcscert.jpipe.compiler.model;

/**
 * A single diagnostic message produced during compilation.
 *
 * <p>
 * {@code line} and {@code column} carry the source location when known; both
 * are {@code 0} when no location is available (ANTLR lines are 1-based, so
 * {@code 0} is a safe sentinel).
 *
 * <p>
 * {@code code} is a stable, bare kebab-case identifier such as
 * {@code "unknown-model"} (see {@link DiagnosticCodes}) or a validation rule
 * name coming from {@link ca.mcscert.jpipe.model.Violation#rule()}. It is
 * {@code null} when the diagnostic has no code. The code is <em>data</em>, not
 * text: renderers decide how to display it (the human-readable report prefixes
 * it as {@code "[code] "}, the JSON report emits it as a separate field).
 *
 * @param level
 *            severity of the diagnostic.
 * @param code
 *            stable kebab-case identifier, or {@code null} when absent.
 * @param source
 *            path to the file that triggered it.
 * @param line
 *            1-based source line, or {@code 0} if unknown.
 * @param column
 *            0-based column offset, or {@code 0} if unknown.
 * @param message
 *            human-readable description, without the code.
 */
public record Diagnostic(Level level, String code, String source, int line,
		int column, String message) {

	/**
	 * Sentinel value for {@code line} and {@code column} when no location is
	 * known.
	 */
	public static final int UNKNOWN_LOCATION = 0;

	public enum Level {
		ERROR, FATAL
	}

	/** True if this diagnostic carries a known source location. */
	public boolean hasLocation() {
		return line > UNKNOWN_LOCATION;
	}

	/** True if this diagnostic carries a code. */
	public boolean hasCode() {
		return code != null && !code.isEmpty();
	}

	// ── without location ────────────────────────────────────────────────────

	public static Diagnostic error(String source, String message) {
		return error(null, source, message);
	}

	public static Diagnostic fatal(String source, String message) {
		return new Diagnostic(Level.FATAL, null, source, UNKNOWN_LOCATION,
				UNKNOWN_LOCATION, message);
	}

	/**
	 * A non-fatal ERROR carrying a code but no source location.
	 *
	 * @param code
	 *            stable kebab-case identifier, or {@code null}.
	 * @param source
	 *            path to the file that triggered it.
	 * @param message
	 *            human-readable description, without the code.
	 * @return the diagnostic.
	 */
	public static Diagnostic error(String code, String source, String message) {
		return new Diagnostic(Level.ERROR, code, source, UNKNOWN_LOCATION,
				UNKNOWN_LOCATION, message);
	}

	// ── with location ────────────────────────────────────────────────────────

	public static Diagnostic error(String source, int line, int column,
			String message) {
		return error(null, source, line, column, message);
	}

	public static Diagnostic fatal(String source, int line, int column,
			String message) {
		return new Diagnostic(Level.FATAL, null, source, line, column, message);
	}

	/**
	 * A non-fatal ERROR carrying both a code and a source location.
	 *
	 * @param code
	 *            stable kebab-case identifier, or {@code null}.
	 * @param source
	 *            path to the file that triggered it.
	 * @param line
	 *            1-based source line.
	 * @param column
	 *            0-based column offset.
	 * @param message
	 *            human-readable description, without the code.
	 * @return the diagnostic.
	 */
	public static Diagnostic error(String code, String source, int line,
			int column, String message) {
		return new Diagnostic(Level.ERROR, code, source, line, column, message);
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
