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

	private int totalDeferrals = 0;

	/** Total number of times a command was deferred across all executions. */
	public int totalDeferrals() {
		return totalDeferrals;
	}

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
				StringBuilder stuck = new StringBuilder(
						"Execution deadlocked — " + commands.size()
								+ " command(s) cannot execute:");
				commands.forEach(c -> stuck.append("\n  stuck: ").append(c));
				throw new IllegalStateException(stuck.toString());
			}
			Command command = commands.removeFirst();
			if (!command.condition().test(unit)) {
				logger.trace("Deferring command [{}]", command);
				commands.add(command);
				deferCount++;
				totalDeferrals++;
			} else if (command instanceof MacroCommand macro) {
				List<Command> expanded;
				try {
					expanded = macro.expand(unit);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new IllegalStateException("Error expanding macro ["
							+ macro + "]: " + e.getMessage(), e);
				}
				logger.debug("Expanding macro [{}] into {} command(s)", macro,
						expanded.size());
				commands.addAll(0, expanded);
				deferCount = 0;
			} else {
				try {
					command.execute(unit);
				} catch (RuntimeException e) {
					throw e;
				} catch (Exception e) {
					throw new IllegalStateException("Error executing command ["
							+ command + "]: " + e.getMessage(), e);
				}
				deferCount = 0;
			}
		}
	}
}
