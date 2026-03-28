package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.MacroCommand;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;

/**
 * Interprets a list of {@link Command}s into a {@link Unit} by executing them
 * through the {@link ExecutionEngine}.
 */
public final class ActionListInterpretation
		extends
			Transformation<List<Command>, Unit> {

	@Override
	protected Unit run(List<Command> input, CompilationContext ctx)
			throws Exception {
		long macros = input.stream().filter(MacroCommand.class::isInstance)
				.count();
		ctx.recordStat("commands.total", input.size());
		ctx.recordStat("commands.macros", macros);

		ExecutionEngine engine = new ExecutionEngine();
		Unit unit = engine.spawn(ctx.sourcePath(), input);

		ctx.recordStat("commands.deferrals", engine.totalDeferrals());
		return unit;
	}
}
