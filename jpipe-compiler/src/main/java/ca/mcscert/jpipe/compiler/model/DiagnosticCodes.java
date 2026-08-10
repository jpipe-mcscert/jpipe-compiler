package ca.mcscert.jpipe.compiler.model;

/**
 * Compiler diagnostic error codes.
 *
 * <p>
 * Each constant is a stable, bare kebab-case identifier carried by
 * {@link Diagnostic#code()}, e.g. {@code "unknown-model"}. Centralising them
 * here makes them greppable and prevents silent drift between the site that
 * emits a diagnostic and any tooling (tests, IDEs) that matches on the code.
 *
 * <p>
 * The identifiers carry no brackets: presentation belongs to the renderer. The
 * human-readable report displays them as {@code "[unknown-model] "} prefixes,
 * while the JSON report emits them as a separate {@code code} field. Validation
 * rules reach {@code Diagnostic.code()} the same way, via
 * {@link ca.mcscert.jpipe.model.Violation#rule()}, and are not listed here.
 */
public final class DiagnosticCodes {

	private DiagnosticCodes() {
	}

	// ---- model construction -------------------------------------------------

	/** A justification model declares more than one conclusion element. */
	public static final String SINGLE_CONCLUSION = "single-conclusion";

	/**
	 * A qualified override reference appears in a model that does not implement
	 * any template.
	 */
	public static final String UNRESOLVED_OVERRIDE = "unresolved-override";

	// ---- template linking ---------------------------------------------------

	/** Two models mutually implement each other, creating a cycle. */
	public static final String CYCLIC_IMPLEMENTS = "cyclic-implements";

	/** An {@code implements} directive could not be applied. */
	public static final String IMPLEMENTS_ERROR = "implements-error";

	/**
	 * A model implementing a template references a template-internal element (a
	 * strategy or conclusion) instead of only overriding its {@code @support}
	 * placeholders.
	 */
	public static final String REFERENCE_INTO_TEMPLATE = "reference-into-template";

	// ---- support / element resolution ---------------------------------------

	/**
	 * An {@code AddSupport} command references an element that does not exist.
	 */
	public static final String INVALID_SUPPORT = "invalid-support";

	/** A command references a model name that does not exist in the unit. */
	public static final String UNKNOWN_MODEL = "unknown-model";

	/** A command references an element ID that does not exist in its model. */
	public static final String UNKNOWN_ELEMENT = "unknown-element";

	// ---- execution ----------------------------------------------------------

	/** A command could not be executed (catch-all for unexpected failures). */
	public static final String EXECUTION_ERROR = "execution-error";

	/**
	 * A command remained unexecuted after all dependency rounds were exhausted.
	 */
	public static final String UNRESOLVED_SYMBOL = "unresolved-symbol";
}
