package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.List;

/**
 * A command that expands into a list of {@link RegularCommand}s at execution
 * time. The {@link ExecutionEngine} calls {@link #expand(Unit)} when the
 * command's {@link #condition()} is satisfied, then splices the result at the
 * front of the queue.
 *
 * <p>
 * {@link MacroCommand}s must never be executed directly; attempting to call
 * {@link #execute(Unit)} throws {@link UnsupportedOperationException}.
 */
public interface MacroCommand extends Command {

	/**
	 * Expands this macro into a sequence of atomic commands to be executed in
	 * order.
	 *
	 * @param context
	 *            the current unit, available to inspect model state.
	 * @return the list of commands to splice into the execution queue.
	 * @throws Exception
	 *             if expansion fails due to an unexpected model state.
	 */
	List<Command> expand(Unit context) throws Exception;

	@Override
	default void execute(Unit context) {
		throw new UnsupportedOperationException(
				"MacroCommand must be expanded by ExecutionEngine, not executed directly");
	}
}
