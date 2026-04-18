package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.DiagnosticCodes;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;

/**
 * Translates command-execution failures into specific
 * {@link CompilationContext} diagnostics.
 *
 * <p>
 * Called by {@link ActionListInterpretation} when the
 * {@link ca.mcscert.jpipe.commands.ExecutionEngine} throws a deadlock or
 * execution exception. Kept separate so that
 * {@code ActionListInterpretation.run()} only handles the happy-path execution
 * flow.
 */
final class ExecutionFailureDiagnostics {

	private ExecutionFailureDiagnostics() {
	}

	/**
	 * Analyses each stuck command from a deadlock and emits a specific
	 * diagnostic explaining why it could not be resolved.
	 */
	static void diagnoseDeadlock(List<Command> stuck, Unit unit,
			CompilationContext ctx) {
		for (Command cmd : stuck) {
			switch (cmd) {
				case AddSupport c -> diagnoseAddSupport(c, unit, ctx);
				case ImplementsTemplate c ->
					diagnoseImplementsTemplate(c, unit, ctx);
				case OverrideAbstractSupport c ->
					diagnoseOverrideAbstractSupport(c, unit, ctx);
				default -> ctx.error(DiagnosticCodes.UNRESOLVED_SYMBOL
						+ " cannot execute: " + cmd);
			}
		}
		ctx.error("unresolved symbol(s) — model cannot be built");
	}

	/**
	 * Analyses a command that raised an exception during execution and emits a
	 * diagnostic that names the specific failure mode.
	 */
	static void diagnoseExecutionFailure(Command cmd, Unit unit,
			Throwable cause, CompilationContext ctx) {
		switch (cmd) {
			case ImplementsTemplate c -> {
				SourceLocation loc = c.location();
				boolean modelHasParent = unit.findModel(c.modelName())
						.flatMap(m -> m.getParent()).isPresent();
				if (modelHasParent) {
					error(ctx, loc,
							DiagnosticCodes.CYCLIC_IMPLEMENTS
									+ " cycle detected: '" + c.modelName()
									+ "' and '" + c.templateName()
									+ "' mutually implement each other");
				} else {
					error(ctx, loc, DiagnosticCodes.IMPLEMENTS_ERROR
							+ " cannot apply 'implements' for '" + c.modelName()
							+ "' extends '" + c.templateName() + "': "
							+ cause.getMessage());
				}
			}
			case AddSupport c -> error(ctx, c.location(),
					DiagnosticCodes.INVALID_SUPPORT + " " + cause.getMessage());
			default -> ctx.error(DiagnosticCodes.EXECUTION_ERROR + " " + cmd
					+ ": " + cause.getMessage());
		}
		ctx.error("model construction failed — see errors above");
	}

	// -------------------------------------------------------------------------
	// Per-command deadlock helpers
	// -------------------------------------------------------------------------

	private static void diagnoseAddSupport(AddSupport c, Unit unit,
			CompilationContext ctx) {
		SourceLocation loc = c.location();
		var modelOpt = unit.findModel(c.container());
		if (modelOpt.isEmpty()) {
			error(ctx, loc, DiagnosticCodes.UNKNOWN_MODEL + " unknown model '"
					+ c.container() + "'");
			return;
		}
		var model = modelOpt.get();
		if (model.findById(c.supportableId()).isEmpty()) {
			error(ctx, loc,
					DiagnosticCodes.UNKNOWN_ELEMENT + " unknown element '"
							+ c.supportableId() + "' in model '" + c.container()
							+ "'");
		}
		if (model.findById(c.supporterId()).isEmpty()) {
			error(ctx, loc,
					DiagnosticCodes.UNKNOWN_ELEMENT + " unknown element '"
							+ c.supporterId() + "' in model '" + c.container()
							+ "'");
		}
	}

	private static void diagnoseImplementsTemplate(ImplementsTemplate c,
			Unit unit, CompilationContext ctx) {
		SourceLocation loc = c.location();
		if (unit.findModel(c.modelName()).isEmpty()) {
			error(ctx, loc, DiagnosticCodes.UNKNOWN_MODEL + " unknown model '"
					+ c.modelName() + "'");
			return;
		}
		if (unit.findModel(c.templateName()).isEmpty()) {
			error(ctx, loc, DiagnosticCodes.UNKNOWN_MODEL + " unknown model '"
					+ c.templateName() + "'");
		}
	}

	private static void diagnoseOverrideAbstractSupport(
			OverrideAbstractSupport c, Unit unit, CompilationContext ctx) {
		SourceLocation loc = c.location();
		var modelOpt = unit.findModel(c.container());
		if (modelOpt.isEmpty()) {
			error(ctx, loc, DiagnosticCodes.UNKNOWN_MODEL + " unknown model '"
					+ c.container() + "'");
			return;
		}
		var model = modelOpt.get();
		if (model.findById(c.qualifiedId()).isEmpty()) {
			error(ctx, loc,
					DiagnosticCodes.UNKNOWN_ELEMENT + " unknown element '"
							+ c.qualifiedId() + "' in model '" + c.container()
							+ "'");
		}
	}

	// -------------------------------------------------------------------------
	// Utility
	// -------------------------------------------------------------------------

	private static void error(CompilationContext ctx, SourceLocation loc,
			String message) {
		if (loc.isKnown()) {
			ctx.error(loc.line(), loc.column(), message);
		} else {
			ctx.error(message);
		}
	}
}
