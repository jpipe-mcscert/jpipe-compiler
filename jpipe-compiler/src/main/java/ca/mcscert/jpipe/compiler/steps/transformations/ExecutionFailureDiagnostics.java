package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.CommandExecutionException;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.AmbiguousIdException;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
			Set<String> failed, CompilationContext ctx) {
		Set<String> neverBuilt = stuck.stream().<String>mapMulti((cmd, out) -> {
			if (cmd instanceof ApplyOperator c) {
				out.accept(c.resultName());
			}
		}).collect(Collectors.toSet());
		// A model whose `implements` is stuck never got its inherited
		// structure: what else waits in it is no separate mistake. The
		// `implements` itself is diagnosed: an unknown model, or a cycle.
		Set<String> unbuilt = new HashSet<>(failed);
		unbuilt.addAll(stuckImplementations(stuck));
		Set<String> broken = explained(stuck, unit, unbuilt, neverBuilt);
		int before = ctx.diagnostics().size();
		for (Command cmd : stuck) {
			if (waitsOn(cmd, broken)) {
				continue;
			}
			switch (cmd) {
				case AddSupport c -> diagnoseAddSupport(c, unit, ctx);
				case ImplementsTemplate c ->
					diagnoseImplementsTemplate(c, unit, stuck, ctx);
				case OverrideAbstractSupport c ->
					diagnoseOverrideAbstractSupport(c, unit, ctx);
				case ApplyOperator c ->
					diagnoseApplyOperator(c, unit, broken, neverBuilt, ctx);
				default -> ctx.error(DiagnosticCodes.UNRESOLVED_SYMBOL,
						cmd.location(), "cannot execute: " + cmd);
			}
		}
		if (ctx.diagnostics().size() > before) {
			ctx.error("unresolved symbol(s) — model cannot be built");
		}
	}

	/**
	 * The models whose absence is already explained: those that {@code failed}
	 * to build, and, until nothing changes, the result of every stuck operator
	 * call with a source that is explained or does not exist at all. Each such
	 * call has a cause that is reported once, at its root (#183).
	 *
	 * <p>
	 * Calls that only wait on one another are left out: nothing else explains
	 * them, so they are reported.
	 */
	private static Set<String> explained(List<Command> stuck, Unit unit,
			Set<String> failed, Set<String> neverBuilt) {
		Set<String> broken = new HashSet<>(failed);
		boolean grew = true;
		while (grew) {
			grew = false;
			for (Command cmd : stuck) {
				if (cmd instanceof ApplyOperator c
						&& !broken.contains(c.resultName())
						&& c.sourceNames().stream()
								.anyMatch(name -> broken.contains(name)
										|| isUnknown(name, unit, neverBuilt))) {
					broken.add(c.resultName());
					grew = true;
				}
			}
		}
		return broken;
	}

	/** The models whose {@code implements} is among the stuck commands. */
	static Set<String> stuckImplementations(List<Command> stuck) {
		return stuck.stream().<String>mapMulti((cmd, out) -> {
			if (cmd instanceof ImplementsTemplate c) {
				out.accept(c.modelName());
			}
		}).collect(Collectors.toSet());
	}

	/** Whether {@code name} is declared nowhere: neither built nor pending. */
	private static boolean isUnknown(String name, Unit unit,
			Set<String> neverBuilt) {
		return unit.findModel(name).isEmpty() && !neverBuilt.contains(name);
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
			// Never skipped: diagnoseImplementsTemplate reports what is its own
			// to report (an unknown model, a cycle) and nothing else.
			// A call is judged source by source: see diagnoseApplyOperator.
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
			// A cycle of `implements` never runs: it deadlocks, and is reported
			// by diagnoseImplementsTemplate. What fails here is the rest, such
			// as a template that is a justification.
			case ImplementsTemplate c ->
				ctx.error(DiagnosticCodes.IMPLEMENTS_ERROR, c.location(),
						"cannot apply 'implements' for '" + c.modelName()
								+ "' extends '" + c.templateName() + "': "
								+ describe(cause));
			case AddSupport c -> {
				String code = switch (cause) {
					case ReferenceIntoTemplateException _ ->
						DiagnosticCodes.REFERENCE_INTO_TEMPLATE;
					case AmbiguousIdException _ -> DiagnosticCodes.AMBIGUOUS_ID;
					default -> DiagnosticCodes.INVALID_SUPPORT;
				};
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
			Unit unit, List<Command> stuck, CompilationContext ctx) {
		SourceLocation loc = c.location();
		if (unit.findModel(c.modelName()).isEmpty()) {
			ctx.error(DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.modelName() + "'");
			return;
		}
		if (unit.findModel(c.templateName()).isEmpty()) {
			ctx.error(DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.templateName() + "'");
			return;
		}
		// Both exist: the template never completes. Either it waits on itself
		// through a chain of `implements`, or on a failure reported elsewhere.
		List<String> cycle = implementsCycle(c, stuck);
		if (!cycle.isEmpty() && cycle.getFirst()
				.equals(cycle.stream().sorted().findFirst().orElseThrow())) {
			ctx.error(DiagnosticCodes.CYCLIC_IMPLEMENTS, loc,
					"cycle detected: " + String.join(" implements ", cycle)
							+ " implements " + cycle.getFirst());
		}
	}

	/**
	 * The models of the {@code implements} cycle starting at {@code c}, in
	 * order, or nothing when the chain of stuck {@code implements} it starts
	 * does not come back to it. A cycle is reported once, from its first model
	 * in alphabetical order.
	 */
	private static List<String> implementsCycle(ImplementsTemplate c,
			List<Command> stuck) {
		List<String> chain = new ArrayList<>(List.of(c.modelName()));
		String next = c.templateName();
		while (!chain.contains(next)) {
			chain.add(next);
			String current = next;
			next = stuck.stream().<ImplementsTemplate>mapMulti((cmd, out) -> {
				if (cmd instanceof ImplementsTemplate i
						&& i.modelName().equals(current)) {
					out.accept(i);
				}
			}).map(ImplementsTemplate::templateName).findFirst().orElse(null);
			if (next == null) {
				return List.of();
			}
		}
		return next.equals(c.modelName()) ? chain : List.of();
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
	 * An operator call waits for its source models. Each source that is missing
	 * is reported at the call, for its own reason: declared nowhere, or
	 * declared by a call that is never built. A source whose absence is already
	 * explained by another failure is not reported again, but the call's other
	 * sources still are.
	 */
	private static void diagnoseApplyOperator(ApplyOperator c, Unit unit,
			Set<String> broken, Set<String> neverBuilt,
			CompilationContext ctx) {
		for (String name : c.sourceNames()) {
			if (unit.findModel(name).isPresent() || broken.contains(name)) {
				continue;
			}
			if (neverBuilt.contains(name)) {
				ctx.error(DiagnosticCodes.UNRESOLVED_SYMBOL, c.location(),
						"cannot apply " + operatorCall(c) + ": it waits on '"
								+ name + "', which is never built");
			} else {
				ctx.error(DiagnosticCodes.UNKNOWN_MODEL, c.location(),
						UNKNOWN_MODEL_MSG + name + "' in the call to "
								+ operatorCall(c));
			}
		}
	}
}
