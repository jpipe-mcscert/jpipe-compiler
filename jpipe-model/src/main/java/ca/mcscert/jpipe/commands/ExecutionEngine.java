package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.ArrayList;
import java.util.Collections;
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
	private final List<ExecutedAction> history = new ArrayList<>();

	private record QueueEntry(Command command, int depth) {
	}

	/** Total number of times a command was deferred across all executions. */
	public int totalDeferrals() {
		return totalDeferrals;
	}

	/** Ordered list of actions that occurred during execution, with depth. */
	public List<ExecutedAction> executedCommands() {
		return Collections.unmodifiableList(history);
	}

	public Unit spawn(String source, List<Command> commands) {
		Unit unit = new Unit(source);
		execute(commands, unit);
		return unit;
	}

	public Unit enrich(Unit context, List<Command> commands) {
		execute(commands, context);
		return context;
	}

	private void execute(List<Command> commands, Unit unit) {
		List<QueueEntry> queue = new ArrayList<>();
		commands.forEach(c -> queue.add(new QueueEntry(c, 0)));
		int deferCount = 0;
		while (!queue.isEmpty()) {
			if (deferCount >= queue.size()) {
				List<Command> stuck = queue.stream().map(QueueEntry::command)
						.toList();
				throw new DeadlockException("Execution deadlocked — "
						+ stuck.size() + " command(s) cannot execute", stuck,
						unit);
			}
			QueueEntry entry = queue.removeFirst();
			Command command = entry.command();
			int depth = entry.depth();
			if (!command.condition().test(unit)) {
				logger.trace("Deferring command [{}]", command);
				queue.add(entry);
				deferCount++;
				totalDeferrals++;
			} else if (command instanceof MacroCommand macro) {
				List<Command> expanded;
				try {
					expanded = macro.expand(unit);
				} catch (Exception e) {
					throw new CommandExecutionException(macro, unit, e);
				}
				logger.debug("Expanding macro [{}] into {} command(s)", macro,
						expanded.size());
				history.add(new ExecutedAction(macro, depth));
				List<QueueEntry> expandedEntries = expanded.stream()
						.map(c -> new QueueEntry(c, depth + 1)).toList();
				queue.addAll(0, expandedEntries);
				deferCount = 0;
			} else {
				try {
					command.execute(unit);
					history.add(new ExecutedAction(command, depth));
				} catch (Exception e) {
					throw new CommandExecutionException(command, unit, e);
				}
				deferCount = 0;
			}
		}
	}
}
