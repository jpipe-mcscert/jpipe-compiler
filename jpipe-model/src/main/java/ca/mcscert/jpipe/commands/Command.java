package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.SourceLocation;
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
