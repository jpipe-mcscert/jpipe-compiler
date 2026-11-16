package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.CommandExecutionException;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.creation.ElementCreationCommand;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.commands.linking.RegisterAlias;
import ca.mcscert.jpipe.commands.linking.ReferenceIntoTemplateException;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.DiagnosticCodes;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
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
				case ApplyOperator c -> diagnoseApplyOperator(c, unit, ctx);
				default -> ctx.error(DiagnosticCodes.UNRESOLVED_SYMBOL,
						locationOf(cmd), "cannot execute: " + cmd);
			}
		}
		ctx.error("unresolved symbol(s) — model cannot be built");
	}

	/**
	 * Reports every command that failed during execution, then one summary. The
	 * engine goes on after a failure, so there can be several (#183).
	 */
	static void diagnoseExecutionFailures(
			List<CommandExecutionException> failures, CompilationContext ctx) {
		if (failures.isEmpty()) {
			return;
		}
		failures.forEach(f -> diagnoseExecutionFailure(f.failedCommand(),
				f.partialUnit(), f.getCause(), ctx));
		ctx.error("model construction failed — see errors above");
	}

	/**
	 * Analyses a command that raised an exception during execution and emits a
	 * diagnostic that names the specific failure mode, at the command's
	 * location.
	 */
	static void diagnoseExecutionFailure(Command cmd, Unit unit,
			Throwable cause, CompilationContext ctx) {
		if (cause instanceof IncompatibleUnificationException) {
			// Raised while expanding an operator call: anchor the diagnostic on
			// the call site rather than on the command's own text.
			ctx.error(DiagnosticCodes.INCOMPATIBLE_UNIFICATION, locationOf(cmd),
					cause.getMessage());
			return;
		}
		switch (cmd) {
			case ImplementsTemplate c -> {
				SourceLocation loc = c.location();
				boolean modelHasParent = unit.findModel(c.modelName())
						.flatMap(m -> m.getParent()).isPresent();
				if (modelHasParent) {
					ctx.error(DiagnosticCodes.CYCLIC_IMPLEMENTS, loc,
							"cycle detected: '" + c.modelName() + "' and '"
									+ c.templateName()
									+ "' mutually implement each other");
				} else {
					ctx.error(DiagnosticCodes.IMPLEMENTS_ERROR, loc,
							"cannot apply 'implements' for '" + c.modelName()
									+ "' extends '" + c.templateName() + "': "
									+ cause.getMessage());
				}
			}
			case AddSupport c -> {
				String code = cause instanceof ReferenceIntoTemplateException
						? DiagnosticCodes.REFERENCE_INTO_TEMPLATE
						: DiagnosticCodes.INVALID_SUPPORT;
				ctx.error(code, c.location(), cause.getMessage());
			}
			case ApplyOperator c -> ctx.error(DiagnosticCodes.EXECUTION_ERROR,
					c.location(), operatorCall(c) + ": " + cause.getMessage());
			default -> {
				SourceLocation loc = locationOf(cmd);
				// The location says which statement failed; without one, the
				// command's text is all there is to go by.
				ctx.error(DiagnosticCodes.EXECUTION_ERROR, loc,
						loc.isKnown()
								? cause.getMessage()
								: cmd + ": " + cause.getMessage());
			}
		}
	}

	/**
	 * Where {@code cmd} is written, for the commands that know it; built-in
	 * atomic commands (expansions of a macro) do not.
	 */
	static SourceLocation locationOf(Command cmd) {
		return switch (cmd) {
			case ElementCreationCommand c -> c.location();
			case CreateJustification c -> c.location();
			case CreateTemplate c -> c.location();
			case AddSupport c -> c.location();
			case ImplementsTemplate c -> c.location();
			case OverrideAbstractSupport c -> c.location();
			case RegisterAlias c -> c.location();
			case ApplyOperator c -> c.location();
			default -> SourceLocation.UNKNOWN;
		};
	}

	/** How an operator call reads in a message: {@code 'refine' for 'r'}. */
	private static String operatorCall(ApplyOperator op) {
		return "'" + op.operatorName() + "' for '" + op.resultName() + "'";
	}

	// -------------------------------------------------------------------------
	// Per-command deadlock helpers
	// -------------------------------------------------------------------------

	private static void diagnoseAddSupport(AddSupport c, Unit unit,
			CompilationContext ctx) {
		SourceLocation loc = c.location();
		var modelOpt = unit.findModel(c.container());
		if (modelOpt.isEmpty()) {
			ctx.error(DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.container() + "'");
			return;
		}
		var model = modelOpt.get();
		if (model.findById(c.supportableId()).isEmpty()) {
			ctx.error(DiagnosticCodes.UNKNOWN_ELEMENT, loc, UNKNOWN_ELEMENT_MSG
					+ c.supportableId() + IN_MODEL + c.container() + "'");
		}
		if (model.findById(c.supporterId()).isEmpty()) {
			ctx.error(DiagnosticCodes.UNKNOWN_ELEMENT, loc, UNKNOWN_ELEMENT_MSG
					+ c.supporterId() + IN_MODEL + c.container() + "'");
		}
	}

	private static void diagnoseImplementsTemplate(ImplementsTemplate c,
			Unit unit, CompilationContext ctx) {
		SourceLocation loc = c.location();
		if (unit.findModel(c.modelName()).isEmpty()) {
			ctx.error(DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.modelName() + "'");
			return;
		}
		if (unit.findModel(c.templateName()).isEmpty()) {
			ctx.error(DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.templateName() + "'");
		}
	}

	private static void diagnoseOverrideAbstractSupport(
			OverrideAbstractSupport c, Unit unit, CompilationContext ctx) {
		SourceLocation loc = c.location();
		var modelOpt = unit.findModel(c.container());
		if (modelOpt.isEmpty()) {
			ctx.error(DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.container() + "'");
			return;
		}
		var model = modelOpt.get();
		var target = model.findById(c.qualifiedId());
		if (target.isEmpty()) {
			ctx.error(DiagnosticCodes.UNKNOWN_ELEMENT, loc, UNKNOWN_ELEMENT_MSG
					+ c.qualifiedId() + IN_MODEL + c.container() + "'");
		} else if (!(target.get() instanceof AbstractSupport)) {
			// The target exists but is concrete: already overridden by an
			// earlier declaration, or never a @support (#183).
			ctx.error(DiagnosticCodes.UNRESOLVED_OVERRIDE, loc, "'"
					+ c.qualifiedId() + IN_MODEL + c.container()
					+ "' cannot be overridden: it is not an abstract support,"
					+ " or it was already overridden");
		}
	}

	/**
	 * An operator call waits for its source models: name the ones that do not
	 * exist, at the call.
	 */
	private static void diagnoseApplyOperator(ApplyOperator c, Unit unit,
			CompilationContext ctx) {
		List<String> missing = c.sourceNames().stream()
				.filter(name -> unit.findModel(name).isEmpty()).toList();
		if (missing.isEmpty()) {
			ctx.error(DiagnosticCodes.UNRESOLVED_SYMBOL, c.location(),
					"cannot apply " + operatorCall(c));
			return;
		}
		missing.forEach(name -> ctx.error(DiagnosticCodes.UNKNOWN_MODEL,
				c.location(), UNKNOWN_MODEL_MSG + name + "' in the call to "
						+ operatorCall(c)));
	}
}
