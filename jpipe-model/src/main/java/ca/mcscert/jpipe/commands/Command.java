package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.function.Predicate;

/**
 * A command in the command pattern for model construction.
 */
public interface Command {

	default Predicate<Unit> condition() {
		return unit -> true;
	}

	void execute(Unit context);
}
