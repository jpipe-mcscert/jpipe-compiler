package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.Unit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Executes a sequence of commands against a {@link Unit}, handling deferred
 * execution for commands whose condition is not yet met.
 *
 * <p>
 * What happens when a command fails is the caller's choice. By default the
 * failure is thrown as a {@link CommandExecutionException} and execution stops.
 * A compiler that wants every error of a source instead hands a failure handler
 * that records it: the failed command is dropped and the rest of the queue
 * still runs, so later commands are not reported as missing only because an
 * earlier one failed.
 */
public final class ExecutionEngine {

	private static final Logger logger = LogManager.getLogger();

	private final Consumer<CommandExecutionException> onFailure;
	private int totalDeferrals = 0;
	private final List<ExecutedAction> history = new ArrayList<>();

	private record QueueEntry(Command command, int depth) {
	}

	/** An engine that stops at the first failing command, throwing it. */
	public ExecutionEngine() {
		this(failure -> {
			throw failure;
		});
	}

	/**
	 * An engine that hands every failing command to {@code onFailure} and goes
	 * on with the rest of the queue, unless the handler throws.
	 */
	public ExecutionEngine(Consumer<CommandExecutionException> onFailure) {
		this.onFailure = onFailure;
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
		PendingIndex pending = new PendingIndex();
		commands.forEach(c -> {
			queue.add(new QueueEntry(c, 0));
			pending.add(c);
		});
		int deferCount = 0;
		while (!queue.isEmpty()) {
			if (deferCount >= queue.size()) {
				runStuck(queue, pending, unit);
				deferCount = 0;
				continue;
			}
			QueueEntry entry = queue.removeFirst();
			if (entry.command().ready(unit, pending.seenBy(entry.command()))) {
				run(entry, queue, pending, unit);
				deferCount = 0;
			} else {
				logger.trace("Deferring command [{}]", entry.command());
				queue.add(entry);
				deferCount++;
				totalDeferrals++;
			}
		}
	}

	/**
	 * Nothing is ready: every command waits on another. Runs, best effort, the
	 * first one that may run without waiting (see
	 * {@link Command#runWhenStuck(Unit)}), so that one mistake does not hold
	 * back everything that waits on it. A valid source never gets here; an
	 * invalid one gets as much built as its mistakes allow, and only those
	 * mistakes reported. When no command may run, execution deadlocks.
	 */
	private void runStuck(List<QueueEntry> queue, PendingIndex pending,
			Unit unit) {
		for (int i = 0; i < queue.size(); i++) {
			if (queue.get(i).command().runWhenStuck(unit)) {
				QueueEntry entry = queue.remove(i);
				logger.debug("Running stuck command [{}]", entry.command());
				run(entry, queue, pending, unit);
				return;
			}
		}
		List<Command> stuck = queue.stream().map(QueueEntry::command).toList();
		throw new DeadlockException("Execution deadlocked — " + stuck.size()
				+ " command(s) cannot execute", stuck, unit);
	}

	/** Expands a macro in place, or executes a command, then forgets it. */
	private void run(QueueEntry entry, List<QueueEntry> queue,
			PendingIndex pending, Unit unit) {
		Command command = entry.command();
		int depth = entry.depth();
		pending.remove(command);
		try {
			if (command instanceof MacroCommand macro) {
				List<Command> expanded = macro.expand(unit);
				logger.debug("Expanding macro [{}] into {} command(s)", macro,
						expanded.size());
				history.add(new ExecutedAction(macro, depth));
				queue.addAll(0, expanded.stream()
						.map(c -> new QueueEntry(c, depth + 1)).toList());
				expanded.forEach(pending::add);
			} else {
				command.execute(unit);
				history.add(new ExecutedAction(command, depth));
			}
		} catch (Exception e) {
			onFailure.accept(new CommandExecutionException(command, unit, e));
		}
	}

	/**
	 * The pending commands, indexed by the model they target, so that a command
	 * asking whether a model is still being changed looks at that model's
	 * commands only, not at the whole queue.
	 */
	private static final class PendingIndex {

		private final Map<String, List<Command>> byTarget = new HashMap<>();

		void add(Command command) {
			command.target()
					.ifPresent(model -> byTarget
							.computeIfAbsent(model, m -> new ArrayList<>())
							.add(command));
		}

		void remove(Command command) {
			command.target().ifPresent(model -> {
				List<Command> commands = byTarget.get(model);
				if (commands != null) {
					commands.removeIf(c -> c == command);
				}
			});
		}

		/** The pending commands as {@code self} sees them: without itself. */
		PendingCommands seenBy(Command self) {
			return (model, except) -> byTarget.getOrDefault(model, List.of())
					.stream().anyMatch(c -> c != self && !except.test(c));
		}
	}
}
