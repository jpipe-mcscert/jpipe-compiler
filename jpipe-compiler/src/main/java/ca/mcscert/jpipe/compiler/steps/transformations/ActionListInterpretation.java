package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.DeadlockException;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.MacroCommand;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.SourceLocation;
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
		ctx.recordStat("commands.total", input.size());
		ctx.recordStat("commands.macros", macros);

		ExecutionEngine engine = new ExecutionEngine();
		Unit unit;
		try {
			unit = engine.spawn(ctx.sourcePath(), input);
		} catch (DeadlockException ex) {
			diagnoseDeadlock(ex.stuckCommands(), ex.partialUnit(), ctx);
			ctx.recordStat("commands.deferrals", engine.totalDeferrals());
			ctx.recordActions(engine.executedCommands());
			return ex.partialUnit();
		}

		ctx.recordStat("commands.deferrals", engine.totalDeferrals());
		ctx.recordActions(engine.executedCommands());
		return unit;
	}

	private void diagnoseDeadlock(List<Command> stuck, Unit unit,
			CompilationContext ctx) {
		for (Command cmd : stuck) {
			switch (cmd) {
				case AddSupport c -> diagnoseAddSupport(c, unit, ctx);
				case ImplementsTemplate c ->
					diagnoseImplementsTemplate(c, unit, ctx);
				case OverrideAbstractSupport c ->
					diagnoseOverrideAbstractSupport(c, unit, ctx);
				default ->
					ctx.error("[unresolved-symbol] cannot execute: " + cmd);
			}
		}
		ctx.error("unresolved symbol(s) — model cannot be built");
	}

	private void diagnoseAddSupport(AddSupport c, Unit unit,
			CompilationContext ctx) {
		SourceLocation loc = c.location();
		if (unit.findModel(c.container()).isEmpty()) {
			error(ctx, loc,
					"[unknown-model] unknown model '" + c.container() + "'");
			return;
		}
		var model = unit.findModel(c.container()).get();
		if (model.findById(c.supportableId()).isEmpty()) {
			error(ctx, loc, "[unknown-element] unknown element '"
					+ c.supportableId() + "' in model '" + c.container() + "'");
		}
		if (model.findById(c.supporterId()).isEmpty()) {
			error(ctx, loc, "[unknown-element] unknown element '"
					+ c.supporterId() + "' in model '" + c.container() + "'");
		}
	}

	private void diagnoseImplementsTemplate(ImplementsTemplate c, Unit unit,
			CompilationContext ctx) {
		SourceLocation loc = c.location();
		if (unit.findModel(c.modelName()).isEmpty()) {
			error(ctx, loc,
					"[unknown-model] unknown model '" + c.modelName() + "'");
			return;
		}
		if (unit.findModel(c.templateName()).isEmpty()) {
			error(ctx, loc,
					"[unknown-model] unknown model '" + c.templateName() + "'");
		}
	}

	private void diagnoseOverrideAbstractSupport(OverrideAbstractSupport c,
			Unit unit, CompilationContext ctx) {
		SourceLocation loc = c.location();
		if (unit.findModel(c.container()).isEmpty()) {
			error(ctx, loc,
					"[unknown-model] unknown model '" + c.container() + "'");
			return;
		}
		var model = unit.findModel(c.container()).get();
		if (model.findById(c.qualifiedId()).isEmpty()) {
			error(ctx, loc, "[unknown-element] unknown element '"
					+ c.qualifiedId() + "' in model '" + c.container() + "'");
		}
	}

	private static void error(CompilationContext ctx, SourceLocation loc,
			String message) {
		if (loc.isKnown()) {
			ctx.error(loc.line(), loc.column(), message);
		} else {
			ctx.error(message);
		}
	}
}
