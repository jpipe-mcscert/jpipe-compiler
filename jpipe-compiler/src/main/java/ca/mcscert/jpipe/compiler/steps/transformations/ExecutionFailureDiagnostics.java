package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.commands.linking.ReferenceIntoTemplateException;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.DiagnosticCodes;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.operators.ApplyOperator;
import ca.mcscert.jpipe.operators.IncompatibleUnificationException;
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

	private static final String UNKNOWN_MODEL_MSG = "unknown model '";
	private static final String UNKNOWN_ELEMENT_MSG = "unknown element '";
	private static final String IN_MODEL = "' in model '";

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
				default -> ctx.error(DiagnosticCodes.UNRESOLVED_SYMBOL,
						"cannot execute: " + cmd);
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
		if (cause instanceof IncompatibleUnificationException) {
			// Raised while expanding an operator call: anchor the diagnostic on
			// the call site rather than on the command's own text.
			SourceLocation loc = cmd instanceof ApplyOperator op
					? op.location()
					: SourceLocation.UNKNOWN;
			error(ctx, DiagnosticCodes.INCOMPATIBLE_UNIFICATION, loc,
					cause.getMessage());
			ctx.error("model construction failed — see errors above");
			return;
		}
		switch (cmd) {
			case ImplementsTemplate c -> {
				SourceLocation loc = c.location();
				boolean modelHasParent = unit.findModel(c.modelName())
						.flatMap(m -> m.getParent()).isPresent();
				if (modelHasParent) {
					error(ctx, DiagnosticCodes.CYCLIC_IMPLEMENTS, loc,
							"cycle detected: '" + c.modelName() + "' and '"
									+ c.templateName()
									+ "' mutually implement each other");
				} else {
					error(ctx, DiagnosticCodes.IMPLEMENTS_ERROR, loc,
							"cannot apply 'implements' for '" + c.modelName()
									+ "' extends '" + c.templateName() + "': "
									+ cause.getMessage());
				}
			}
			case AddSupport c -> {
				String code = cause instanceof ReferenceIntoTemplateException
						? DiagnosticCodes.REFERENCE_INTO_TEMPLATE
						: DiagnosticCodes.INVALID_SUPPORT;
				error(ctx, code, c.location(), cause.getMessage());
			}
			default -> ctx.error(DiagnosticCodes.EXECUTION_ERROR,
					cmd + ": " + cause.getMessage());
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
			error(ctx, DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.container() + "'");
			return;
		}
		var model = modelOpt.get();
		if (model.findById(c.supportableId()).isEmpty()) {
			error(ctx, DiagnosticCodes.UNKNOWN_ELEMENT, loc, UNKNOWN_ELEMENT_MSG
					+ c.supportableId() + IN_MODEL + c.container() + "'");
		}
		if (model.findById(c.supporterId()).isEmpty()) {
			error(ctx, DiagnosticCodes.UNKNOWN_ELEMENT, loc, UNKNOWN_ELEMENT_MSG
					+ c.supporterId() + IN_MODEL + c.container() + "'");
		}
	}

	private static void diagnoseImplementsTemplate(ImplementsTemplate c,
			Unit unit, CompilationContext ctx) {
		SourceLocation loc = c.location();
		if (unit.findModel(c.modelName()).isEmpty()) {
			error(ctx, DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.modelName() + "'");
			return;
		}
		if (unit.findModel(c.templateName()).isEmpty()) {
			error(ctx, DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.templateName() + "'");
		}
	}

	private static void diagnoseOverrideAbstractSupport(
			OverrideAbstractSupport c, Unit unit, CompilationContext ctx) {
		SourceLocation loc = c.location();
		var modelOpt = unit.findModel(c.container());
		if (modelOpt.isEmpty()) {
			error(ctx, DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.container() + "'");
			return;
		}
		var model = modelOpt.get();
		if (model.findById(c.qualifiedId()).isEmpty()) {
			error(ctx, DiagnosticCodes.UNKNOWN_ELEMENT, loc, UNKNOWN_ELEMENT_MSG
					+ c.qualifiedId() + IN_MODEL + c.container() + "'");
		}
	}

	// -------------------------------------------------------------------------
	// Utility
	// -------------------------------------------------------------------------

	private static void error(CompilationContext ctx, String code,
			SourceLocation loc, String message) {
		if (loc.isKnown()) {
			ctx.error(code, loc.line(), loc.column(), message);
		} else {
			ctx.error(code, message);
		}
	}
}
