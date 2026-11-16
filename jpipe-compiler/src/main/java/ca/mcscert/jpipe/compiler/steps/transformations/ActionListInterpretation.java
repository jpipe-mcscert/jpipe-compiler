package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.CommandExecutionException;
import ca.mcscert.jpipe.commands.DeadlockException;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.MacroCommand;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.Unit;
import java.util.ArrayList;
import java.util.List;

/**
 * Interprets a list of {@link Command}s into a {@link Unit} by executing them
 * through the {@link ExecutionEngine}.
 *
 * <p>
 * A command that fails is reported and the others still run. If execution
 * deadlocks, each stuck command is analysed to emit a specific semantic
 * diagnostic (e.g. {@code [unknown-element]}) rather than propagating an
 * unexpected exception.
 */
public final class ActionListInterpretation
		extends
			Transformation<List<Command>, Unit> {

	@Override
	protected Unit run(List<Command> input, CompilationContext ctx) {
		long macros = input.stream().filter(MacroCommand.class::isInstance)
				.count();
		ctx.recordStat(CompilationContext.STAT_COMMANDS_TOTAL, input.size());
		ctx.recordStat(CompilationContext.STAT_COMMANDS_MACROS, macros);

		// A failing command is recorded and the rest still run, so that one
		// error does not hide the others, or make the checkers report as
		// missing what the source declares (#183).
		List<CommandExecutionException> failures = new ArrayList<>();
		ExecutionEngine engine = new ExecutionEngine(failures::add);
		Unit unit;
		DeadlockException deadlock = null;
		try {
			unit = engine.spawn(ctx.sourcePath(), input);
		} catch (DeadlockException ex) {
			deadlock = ex;
			unit = ex.partialUnit();
		}
		ExecutionFailureDiagnostics.diagnoseExecutionFailures(failures, ctx);
		if (deadlock != null) {
			ExecutionFailureDiagnostics
					.diagnoseDeadlock(deadlock.stuckCommands(), unit, ctx);
		}

		ctx.recordStat(CompilationContext.STAT_COMMANDS_DEFERRALS,
				engine.totalDeferrals());
		ctx.recordActions(engine.executedCommands());
		return unit;
	}
}
