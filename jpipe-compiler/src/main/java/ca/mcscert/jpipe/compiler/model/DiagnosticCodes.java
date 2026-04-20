package ca.mcscert.jpipe.compiler.model;

/**
 * Compiler diagnostic error-code tags embedded in error messages.
 *
 * <p>
 * Each constant is the bracketed tag that appears at the start of a diagnostic
 * message, e.g. {@code "[unknown-model] unknown model 'foo'"}. Centralising
 * them here makes them greppable and prevents silent drift between the site
 * that emits a diagnostic and any tooling (tests, IDEs) that matches on the tag
 * string.
 */
public final class DiagnosticCodes {

	private DiagnosticCodes() {
	}

	// ---- model construction -------------------------------------------------

	/** A justification model declares more than one conclusion element. */
	public static final String SINGLE_CONCLUSION = "[single-conclusion]";

	/**
	 * A qualified override reference appears in a model that does not implement
	 * any template.
	 */
	public static final String UNRESOLVED_OVERRIDE = "[unresolved-override]";

	// ---- template linking ---------------------------------------------------

	/** Two models mutually implement each other, creating a cycle. */
	public static final String CYCLIC_IMPLEMENTS = "[cyclic-implements]";

	/** An {@code implements} directive could not be applied. */
	public static final String IMPLEMENTS_ERROR = "[implements-error]";

	// ---- support / element resolution ---------------------------------------

	/**
	 * An {@code AddSupport} command references an element that does not exist.
	 */
	public static final String INVALID_SUPPORT = "[invalid-support]";

	/** A command references a model name that does not exist in the unit. */
	public static final String UNKNOWN_MODEL = "[unknown-model]";

	/** A command references an element ID that does not exist in its model. */
	public static final String UNKNOWN_ELEMENT = "[unknown-element]";

	// ---- execution ----------------------------------------------------------

	/** A command could not be executed (catch-all for unexpected failures). */
	public static final String EXECUTION_ERROR = "[execution-error]";

	/**
	 * A command remained unexecuted after all dependency rounds were exhausted.
	 */
	public static final String UNRESOLVED_SYMBOL = "[unresolved-symbol]";
}
