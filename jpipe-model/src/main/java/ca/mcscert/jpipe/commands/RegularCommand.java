package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class for commands that execute directly on a {@link Unit}.
 */
public abstract class RegularCommand implements Command {

	protected static final Logger logger = LogManager.getLogger();

	@Override
	public abstract void execute(Unit context) throws Exception;
}
