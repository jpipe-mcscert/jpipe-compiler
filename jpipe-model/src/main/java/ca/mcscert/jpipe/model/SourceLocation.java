package ca.mcscert.jpipe.model;

/**
 * Immutable source position at which a named element was declared.
 *
 * <p>
 * Line is 1-based (ANTLR convention); column is 0-based. The sentinel
 * {@link #UNKNOWN} (line = 0) is used when no position is available — ANTLR
 * lines start at 1, so 0 is a safe sentinel that never conflicts with a real
 * location.
 *
 * <p>
 * The optional {@code source} field names the file (or unit) the location
 * belongs to. It is {@code null} when the source is not tracked (e.g. in tests
 * or for programmatically created locations). When the load mechanism assembles
 * units from multiple files each location will carry its own source path,
 * making cross-file error messages unambiguous.
 */
public record SourceLocation(String source, int line, int column) {

	/** Sentinel for "no location known". */
	public static final SourceLocation UNKNOWN = new SourceLocation(0, 0);

	/** Convenience constructor for location without a source file reference. */
	public SourceLocation(int line, int column) {
		this(null, line, column);
	}

	/** True only when a real source position is stored. */
	public boolean isKnown() {
		return line > 0;
	}

	@Override
	public String toString() {
		if (!isKnown()) {
			return "<unknown>";
		}
		return source != null
				? source + ":" + line + ":" + column
				: line + ":" + column;
	}
}
