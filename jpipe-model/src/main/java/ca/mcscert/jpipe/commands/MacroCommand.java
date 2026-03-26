package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.List;

/**
 * A composite command that expands into a list of commands rather than
 * executing directly.
 */
public abstract class MacroCommand implements Command {

	@Override
	public final boolean isExpandable() {
		return true;
	}

	@Override
	public final void execute(Unit context) {
		throw new UnsupportedOperationException("Macro command cannot be executed directly");
	}

	@Override
	public abstract List<Command> expand(Unit context) throws Exception;
}
