package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.function.Function;

/**
 * A command in the command pattern for model construction.
 */
public interface Command {

	default Function<Unit, Boolean> condition() {
		return (unit -> Boolean.TRUE);
	}

	void execute(Unit context) throws Exception;
}
