package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.List;

/**
 * Thrown by {@link ExecutionEngine} when execution deadlocks — all remaining
 * commands have unsatisfied conditions and cannot make progress.
 *
 * <p>
 * Carries the stuck commands and the partially-built {@link Unit} so callers
 * can introspect which symbols were unresolvable.
 */
public final class DeadlockException extends RuntimeException {

	private final List<Command> stuckCommands;
	private final Unit partialUnit;

	public DeadlockException(String message, List<Command> stuckCommands,
			Unit partialUnit) {
		super(message);
		this.stuckCommands = List.copyOf(stuckCommands);
		this.partialUnit = partialUnit;
	}

	/** Commands that could not execute due to unsatisfied conditions. */
	public List<Command> stuckCommands() {
		return stuckCommands;
	}

	/** The unit as it was at the moment of deadlock. */
	public Unit partialUnit() {
		return partialUnit;
	}
}
