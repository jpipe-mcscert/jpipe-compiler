package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.CommandExecutionException;
import ca.mcscert.jpipe.commands.DeadlockException;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.MacroCommand;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;

/**
 * Interprets a list of {@link Command}s into a {@link Unit} by executing them
 * through the {@link ExecutionEngine}.
 *
 * <p>
 * If execution deadlocks, each stuck command is analysed to emit a specific
 * semantic diagnostic (e.g. {@code [unknown-element]}) rather than propagating
 * an unexpected exception.
 */
public final class ActionListInterpretation
		extends
			Transformation<List<Command>, Unit> {

	@Override
	protected Unit run(List<Command> input, CompilationContext ctx)
			throws Exception {
		long macros = input.stream().filter(MacroCommand.class::isInstance)
				.count();
		ctx.recordStat(CompilationContext.STAT_COMMANDS_TOTAL, input.size());
		ctx.recordStat(CompilationContext.STAT_COMMANDS_MACROS, macros);

		ExecutionEngine engine = new ExecutionEngine();
		Unit unit;
		try {
			unit = engine.spawn(ctx.sourcePath(), input);
		} catch (DeadlockException ex) {
			ExecutionFailureDiagnostics.diagnoseDeadlock(ex.stuckCommands(),
					ex.partialUnit(), ctx);
			ctx.recordStat(CompilationContext.STAT_COMMANDS_DEFERRALS,
					engine.totalDeferrals());
			ctx.recordActions(engine.executedCommands());
			return ex.partialUnit();
		} catch (CommandExecutionException ex) {
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(
					ex.failedCommand(), ex.partialUnit(), ex.getCause(), ctx);
			ctx.recordStat(CompilationContext.STAT_COMMANDS_DEFERRALS,
					engine.totalDeferrals());
			ctx.recordActions(engine.executedCommands());
			return ex.partialUnit();
		}

		ctx.recordStat(CompilationContext.STAT_COMMANDS_DEFERRALS,
				engine.totalDeferrals());
		ctx.recordActions(engine.executedCommands());
		return unit;
	}
}
