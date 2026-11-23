package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * The commands still waiting in the execution queue, as a command deciding
 * whether it is ready sees them: everything pending except itself.
 */
@FunctionalInterface
public interface PendingCommands {

	/** No command pending: what a command sees when nothing holds it back. */
	PendingCommands NONE = (model, except) -> false;

	/**
	 * Whether some pending command creates or changes {@code model} (see
	 * {@link Command#target()}), other than those {@code except} accepts.
	 */
	boolean target(String model, Predicate<? super Command> except);

	/** Whether some pending command creates or changes {@code model}. */
	default boolean target(String model) {
		return target(model, c -> false);
	}

	/**
	 * Whether every model of {@code models} is settled: it exists, no pending
	 * command still changes it, and it was built in full. A command that reads
	 * these models then sees what they are, not what they are so far (#177).
	 */
	default boolean settled(Unit unit, Collection<String> models) {
		return models.stream().allMatch(m -> unit.findModel(m).isPresent()
				&& !target(m) && unit.isComplete(m));
	}
}
