package ca.mcscert.jpipe.compiler;

/**
 * Output formats of the {@code diagnostic} command's report.
 *
 * <p>
 * Deliberately distinct from {@link Format}, which enumerates <em>model
 * export</em> targets (DOT, PNG, Python, …): none of those mean anything for a
 * compilation report, and conflating the two would offer combinations that
 * cannot be produced.
 */
public enum DiagnosticFormat {

	/** Human-readable report, for terminal use. This is the default. */
	TEXT,

	/** Machine-readable report, for IDEs and other tooling. */
	JSON
}
