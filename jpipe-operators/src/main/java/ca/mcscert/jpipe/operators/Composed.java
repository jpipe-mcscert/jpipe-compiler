package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.commands.Command;
import java.util.List;

/**
 * What {@link CompositionOperator#apply} produced: the kind of model the
 * operator chose for the result, and the commands that build it, the command
 * creating the result model first.
 */
public record Composed(ModelKind kind, List<Command> commands) {

	public Composed {
		commands = List.copyOf(commands);
	}
}
