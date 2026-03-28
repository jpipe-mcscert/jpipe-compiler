package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.ArrayList;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Executes a sequence of commands against a {@link Unit}, handling deferred
 * execution for commands whose condition is not yet met.
 */
public final class ExecutionEngine {

	private static final Logger logger = LogManager.getLogger();

	public Unit spawn(String source, List<Command> commands) {
		Unit unit = new Unit(source);
		execute(new ArrayList<>(commands), unit);
		return unit;
	}

	public Unit enrich(Unit context, List<Command> commands) {
		execute(new ArrayList<>(commands), context);
		return context;
	}

	private void execute(List<Command> commands, Unit unit) {
		int deferCount = 0;
		while (!commands.isEmpty()) {
			if (deferCount >= commands.size()) {
				logger.error("Execution deadlocked — {} command(s) stuck:",
						commands.size());
				commands.forEach(c -> logger.error("  stuck: {}", c));
				return;
			}
			Command command = commands.removeFirst();
			if (!command.condition().test(unit)) {
				logger.trace("Deferring command [{}]", command);
				commands.add(command);
				deferCount++;
			} else if (command instanceof MacroCommand macro) {
				try {
					List<Command> expanded = macro.expand(unit);
					logger.debug("Expanding macro [{}] into {} command(s)",
							macro, expanded.size());
					commands.addAll(0, expanded);
				} catch (Exception e) {
					logger.error("Error expanding macro [{}]: {}", macro,
							e.getMessage());
				}
				deferCount = 0;
			} else {
				try {
					command.execute(unit);
				} catch (Exception e) {
					logger.error("Error executing command [{}]: {}", command,
							e.getMessage());
				}
				deferCount = 0;
			}
		}
	}
}
