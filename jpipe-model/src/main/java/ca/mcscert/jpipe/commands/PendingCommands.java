package ca.mcscert.jpipe.commands;

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
}
