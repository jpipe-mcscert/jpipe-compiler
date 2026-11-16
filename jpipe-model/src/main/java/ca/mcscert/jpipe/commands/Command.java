package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * A command in the command pattern for model construction.
 */
public interface Command {

	default Predicate<Unit> condition() {
		return unit -> true;
	}

	/**
	 * Whether this command can run now. By default, when its
	 * {@link #condition()} holds on the unit. A command whose outcome depends
	 * on a model being complete also waits until no pending command still
	 * targets that model, so that the meaning of a source does not depend on
	 * the order of its declarations (#177).
	 *
	 * @param unit
	 *            the unit built so far.
	 * @param pending
	 *            the commands still waiting, this one excluded.
	 */
	default boolean ready(Unit unit, PendingCommands pending) {
		return condition().test(unit);
	}

	/**
	 * The model this command creates or changes, if any. Other commands read it
	 * to wait until a model is complete.
	 */
	default Optional<String> target() {
		return Optional.empty();
	}

	void execute(Unit context);

	/**
	 * Where the statement this command comes from is written, so that a failure
	 * of the command is reported there. Commands created by the compiler rather
	 * than read from a source, such as the expansion of a macro, have none.
	 *
	 * @return the location, or {@link SourceLocation#UNKNOWN}.
	 */
	default SourceLocation location() {
		return SourceLocation.UNKNOWN;
	}
}
