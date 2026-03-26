package ca.mcscert.jpipe.compiler.model;

/**
 * A single diagnostic message produced during compilation.
 *
 * @param level
 *            severity of the diagnostic.
 * @param source
 *            path to the file that triggered it.
 * @param message
 *            human-readable description.
 */
public record Diagnostic(Level level, String source, String message) {

	public enum Level {
		WARNING, ERROR, FATAL
	}

	public static Diagnostic warning(String source, String message) {
		return new Diagnostic(Level.WARNING, source, message);
	}

	public static Diagnostic error(String source, String message) {
		return new Diagnostic(Level.ERROR, source, message);
	}

	public static Diagnostic fatal(String source, String message) {
		return new Diagnostic(Level.FATAL, source, message);
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
