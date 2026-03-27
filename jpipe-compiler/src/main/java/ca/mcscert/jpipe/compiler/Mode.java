package ca.mcscert.jpipe.compiler;

/**
 * Compiler operating mode.
 *
 * <ul>
 * <li>{@link #DIAGNOSTIC} — parse and report diagnostics only; no model is
 * built or exported.
 * <li>{@link #PROCESS} — full pipeline: parse, build model, validate, and
 * export.
 * </ul>
 */
public enum Mode {
	DIAGNOSTIC, PROCESS
}
