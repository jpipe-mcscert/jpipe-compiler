package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * An atomic command that executes directly on a {@link Unit}.
 */
public abstract class RegularCommand implements Command {

	protected static final Logger logger = LogManager.getLogger();

	@Override
	public final boolean isExpandable() {
		return false;
	}

	@Override
	public abstract void execute(Unit context) throws Exception;

	@Override
	public final List<Command> expand(Unit context) {
		throw new UnsupportedOperationException("Regular command cannot be expanded");
	}
}
