package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.CommandExecutionException;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.commands.linking.ReferenceIntoTemplateException;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.DiagnosticCodes;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.operators.ApplyOperator;
import ca.mcscert.jpipe.operators.IncompatibleUnificationException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Translates command-execution failures into specific
 * {@link CompilationContext} diagnostics.
 *
 * <p>
 * Called by {@link ActionListInterpretation} with the commands that failed,
 * which the {@link ca.mcscert.jpipe.commands.ExecutionEngine} hands to its
 * failure handler, and with the commands left stuck when it deadlocks. Kept
 * separate so that {@code ActionListInterpretation.run()} only handles the
 * execution flow.
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
		diagnoseDeadlock(stuck, unit, Set.of(), ctx);
	}

	/**
	 * As {@link #diagnoseDeadlock(List, Unit, CompilationContext)}, leaving out
	 * the commands that wait on a model in {@code broken}: one whose operator
	 * call or {@code implements} failed. That failure is already reported, and
	 * what waits on it is no separate mistake (#183).
	 */
	static void diagnoseDeadlock(List<Command> stuck, Unit unit,
			Set<String> broken, CompilationContext ctx) {
		List<Command> reported = stuck.stream()
				.filter(cmd -> !waitsOn(cmd, broken)).toList();
		if (reported.isEmpty()) {
			return;
		}
		for (Command cmd : reported) {
			switch (cmd) {
				case AddSupport c -> diagnoseAddSupport(c, unit, ctx);
				case ImplementsTemplate c ->
					diagnoseImplementsTemplate(c, unit, ctx);
				case OverrideAbstractSupport c ->
					diagnoseOverrideAbstractSupport(c, unit, ctx);
				case ApplyOperator c -> diagnoseApplyOperator(c, unit, ctx);
				default -> ctx.error(DiagnosticCodes.UNRESOLVED_SYMBOL,
						cmd.location(), "cannot execute: " + cmd);
			}
		}
		ctx.error("unresolved symbol(s) — model cannot be built");
	}

	/**
	 * The models a failing command leaves unusable: the result of an operator
	 * call, a model whose template could not be implemented.
	 */
	static Set<String> brokenModels(List<CommandExecutionException> failures) {
		Set<String> broken = new HashSet<>();
		for (CommandExecutionException f : failures) {
			switch (f.failedCommand()) {
				case ApplyOperator c -> broken.add(c.resultName());
				case ImplementsTemplate c -> broken.add(c.modelName());
				default -> {
					// The model is still whole: what waits on it is reported.
				}
			}
		}
		return broken;
	}

	/** Whether {@code cmd} is stuck only because it uses a broken model. */
	private static boolean waitsOn(Command cmd, Set<String> broken) {
		return switch (cmd) {
			case AddSupport c -> broken.contains(c.container());
			case OverrideAbstractSupport c -> broken.contains(c.container());
			case ImplementsTemplate c -> broken.contains(c.modelName())
					|| broken.contains(c.templateName());
			case ApplyOperator c ->
				c.sourceNames().stream().anyMatch(broken::contains);
			default -> false;
		};
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
			ctx.error(DiagnosticCodes.INCOMPATIBLE_UNIFICATION, cmd.location(),
					describe(cause));
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
									+ describe(cause));
				}
			}
			case AddSupport c -> {
				String code = cause instanceof ReferenceIntoTemplateException
						? DiagnosticCodes.REFERENCE_INTO_TEMPLATE
						: DiagnosticCodes.INVALID_SUPPORT;
				ctx.error(code, c.location(), describe(cause));
			}
			case ApplyOperator c -> ctx.error(DiagnosticCodes.EXECUTION_ERROR,
					c.location(), operatorCall(c) + ": " + describe(cause));
			default -> {
				SourceLocation loc = cmd.location();
				// The location says which statement failed; without one, the
				// command's text is all there is to go by.
				ctx.error(DiagnosticCodes.EXECUTION_ERROR, loc,
						loc.isKnown()
								? describe(cause)
								: cmd + ": " + describe(cause));
			}
		}
	}

	/**
	 * What went wrong, in the exception's words, or its type when it has none:
	 * a {@code NullPointerException} may carry no message.
	 */
	private static String describe(Throwable cause) {
		return cause.getMessage() != null
				? cause.getMessage()
				: cause.getClass().getSimpleName();
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
