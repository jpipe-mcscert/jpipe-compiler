package ca.mcscert.jpipe.commands;

/**
 * A record of a single action that took place during execution: either a
 * {@link RegularCommand} that was executed, or a {@link MacroCommand} that was
 * expanded. The {@code depth} field reflects nesting level — top-level commands
 * have depth 0; commands produced by expanding a macro inherit the macro's
 * depth + 1.
 */
public record ExecutedAction(Command command, int depth) {

	/**
	 * True when this entry represents a macro expansion rather than direct
	 * execution.
	 */
	public boolean isMacro() {
		return command instanceof MacroCommand;
	}
}
