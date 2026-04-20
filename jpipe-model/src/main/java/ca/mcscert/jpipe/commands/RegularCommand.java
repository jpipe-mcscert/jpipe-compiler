package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class for commands that execute directly on a {@link Unit}.
 *
 * <p>
 * Subclasses implement {@link #doExecute(Unit)} rather than {@code execute}.
 * This class logs each execution at DEBUG level under the concrete subclass
 * name before delegating.
 */
public abstract class RegularCommand implements Command {

	private final Logger logger = LogManager.getLogger(getClass());

	@Override
	public final void execute(Unit context) {
		logger.debug("Executing {}", this);
		doExecute(context);
	}

	protected abstract void doExecute(Unit context);
}
