package ca.mcscert.jpipe.commands;

import java.util.function.Predicate;

/**
 * The commands still waiting in the execution queue, as a command deciding
 * whether it is ready sees them: everything pending except itself.
 */
@FunctionalInterface
public interface PendingCommands {

	/** Whether some pending command satisfies {@code filter}. */
	boolean any(Predicate<? super Command> filter);

	/**
	 * Whether some pending command creates or changes {@code model}, other than
	 * those {@code except} accepts.
	 */
	default boolean target(String model, Predicate<? super Command> except) {
		return any(c -> c.target().filter(model::equals).isPresent()
				&& !except.test(c));
	}

	/** Whether some pending command creates or changes {@code model}. */
	default boolean target(String model) {
		return target(model, c -> false);
	}
}
