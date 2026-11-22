package ca.mcscert.jpipe.commands;

import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.util.QualifiedIds;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A command in the command pattern for model construction.
 */
public interface Command {

	default Predicate<Unit> condition() {
		return unit -> true;
	}

	/**
	 * Whether this command can run now. By default, when its
	 * {@link #condition()} holds on the unit. A command whose outcome depends
	 * on a model being complete also waits until no pending command still
	 * targets that model, so that the meaning of a source does not depend on
	 * the order of its declarations (#177).
	 *
	 * @param unit
	 *            the unit built so far.
	 * @param pending
	 *            the commands still waiting, this one excluded.
	 */
	default boolean ready(Unit unit, PendingCommands pending) {
		return condition().test(unit);
	}

	/**
	 * Whether this command may still run once nothing in the queue is ready,
	 * every command waiting on another. The engine then runs, best effort, one
	 * that may, so that one mistake does not hold back everything waiting on
	 * it: the first that {@linkplain #reads() reads} no model another of them
	 * may still change, or else the first. By default, when its
	 * {@link #condition()} holds: what it needs exists, complete or not. A
	 * command that must never act on an incomplete model refuses.
	 */
	default boolean runWhenStuck(Unit unit, PendingCommands pending) {
		return condition().test(unit);
	}

	/**
	 * The models this command reads, beyond the one it {@linkplain #target()
	 * targets}. When nothing is ready, the engine runs what may still change a
	 * model before a command that reads it, so that a reader acting on an
	 * incomplete model gets as much of it as its mistakes allow.
	 */
	default Set<String> reads() {
		return Set.of();
	}

	/**
	 * The model this command creates or changes, if any. Other commands read it
	 * to wait until a model is complete.
	 */
	default Optional<String> target() {
		return Optional.empty();
	}

	void execute(Unit context);

	/**
	 * This command, read from a file loaded under namespace {@code ns}: every
	 * model name it holds is qualified by {@code ns}, and every qualified
	 * element id too (see {@link QualifiedIds#inNamespace}); a plain element
	 * id, a label or a type is unchanged. Each command type states how it is
	 * namespaced, so that none escapes the namespace silently. A command the
	 * compiler creates while building the unit is never read from a file, and
	 * refuses.
	 *
	 * @throws UnsupportedOperationException
	 *             if this command is never read from a file
	 */
	Command withNamespace(String ns);

	/**
	 * The model this command declares, if it starts a model's declaration: a
	 * justification, a template, or the result of an operator call.
	 */
	default Optional<String> declares() {
		return Optional.empty();
	}

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
