package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;

/**
 * Thrown by {@link ExecutionEngine} when a command's condition is satisfied but
 * its execution throws a {@link RuntimeException}.
 *
 * <p>
 * Carries the command that failed and the partially-built {@link Unit} so
 * callers can emit a meaningful diagnostic instead of propagating the raw
 * exception.
 */
public final class CommandExecutionException extends RuntimeException {

	private final Command failedCommand;
	private final Unit partialUnit;

	public CommandExecutionException(Command failedCommand, Unit partialUnit,
			Throwable cause) {
		super(cause.getMessage(), cause);
		this.failedCommand = failedCommand;
		this.partialUnit = partialUnit;
	}

	/** The command whose execution failed. */
	public Command failedCommand() {
		return failedCommand;
	}

	/** The unit as it was at the moment of failure. */
	public Unit partialUnit() {
		return partialUnit;
	}
}
