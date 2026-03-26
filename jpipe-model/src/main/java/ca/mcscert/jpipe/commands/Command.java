package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import java.util.function.Function;

/**
 * A command in the command pattern for model construction. Commands are either
 * atomic ({@link RegularCommand}) or composite ({@link MacroCommand}).
 */
public interface Command {

	boolean isExpandable();

	default Function<Unit, Boolean> condition() {
		return (unit -> Boolean.TRUE);
	}

	void execute(Unit context) throws Exception;

	List<Command> expand(Unit context) throws Exception;
}
