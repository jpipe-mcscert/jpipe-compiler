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
import ca.mcscert.jpipe.operators.InvalidOperatorCallException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
		// The models a stuck command declares: results of operator calls.
		Set<String> neverBuilt = stuck.stream()
				.flatMap(cmd -> cmd.declares().stream())
				.collect(Collectors.toSet());
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
				case ImplementsTemplate c -> diagnoseImplementsTemplate(c, unit,
						stuck, broken, neverBuilt, ctx);
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
	 * to build, and, until nothing changes, the target of every stuck command
	 * that {@linkplain Command#breaksItsTarget() breaks it} and reads a model
	 * that is explained or does not exist at all: the result of an operator
	 * call. Each such command has a cause that is reported once, at its root
	 * (#183).
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
				Optional<String> target = cmd.target();
				if (cmd.breaksItsTarget() && target.isPresent()
						&& !broken.contains(target.get())
						&& cmd.reads().stream()
								.anyMatch(name -> broken.contains(name)
										|| isUnknown(name, unit, neverBuilt))) {
					broken.add(target.get());
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
	 * The models a failing command leaves unusable: the target of each that
	 * {@linkplain Command#breaksItsTarget() breaks it}, the result of an
	 * operator call, a model whose template could not be implemented. Any other
	 * model is still whole: what waits on it is reported.
	 */
	static Set<String> brokenModels(List<CommandExecutionException> failures) {
		return failures.stream().map(CommandExecutionException::failedCommand)
				.filter(Command::breaksItsTarget)
				.flatMap(cmd -> cmd.target().stream())
				.collect(Collectors.toSet());
	}

	/**
	 * Whether {@code cmd} is stuck only because it changes a broken model. A
	 * command that gives a model its structure is never skipped: an
	 * {@code implements} reports what is its own to report (an unknown model, a
	 * cycle) and nothing else, and a call is judged source by source (see
	 * diagnoseApplyOperator).
	 */
	private static boolean waitsOn(Command cmd, Set<String> broken) {
		return !cmd.breaksItsTarget()
				&& cmd.target().filter(broken::contains).isPresent();
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
				f.getCause(), ctx));
		ctx.error("model construction failed — see errors above");
	}

	/**
	 * Analyses a command that raised an exception during execution and emits a
	 * diagnostic that names the specific failure mode, at the command's
	 * location.
	 */
	static void diagnoseExecutionFailure(Command cmd, Throwable cause,
			CompilationContext ctx) {
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
			// An override whose short id names several inherited @supports.
			case OverrideAbstractSupport c when cause instanceof AmbiguousIdException ->
				ctx.error(DiagnosticCodes.AMBIGUOUS_ID, c.location(),
						describe(cause));
			// A call written in a way it cannot honour is
			// invalid-operator-call,
			// as when the parser finds it; anything else is an execution error.
			case ApplyOperator c -> ctx.error(
					cause instanceof InvalidOperatorCallException
							? DiagnosticCodes.INVALID_OPERATOR_CALL
							: DiagnosticCodes.EXECUTION_ERROR,
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

	/**
	 * An {@code implements} waits for its template to be complete. It reports a
	 * template declared nowhere, and the cycle it belongs to, once. A template
	 * that an operator call builds, and that is not built, is no mistake of the
	 * {@code implements}: the call reports why, or the failure it waits on does
	 * (#183).
	 */
	private static void diagnoseImplementsTemplate(ImplementsTemplate c,
			Unit unit, List<Command> stuck, Set<String> broken,
			Set<String> neverBuilt, CompilationContext ctx) {
		SourceLocation loc = c.location();
		if (unit.findModel(c.modelName()).isEmpty()) {
			ctx.error(DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + c.modelName() + "'");
			return;
		}
		List<String> cycle = cycleThrough(c, stuck);
		if (!cycle.isEmpty()) {
			if (reportsCycle(c, cycle, stuck)) {
				ctx.error(DiagnosticCodes.CYCLIC_IMPLEMENTS, loc,
						"cycle detected: " + describeCycle(cycle, stuck));
			}
			return;
		}
		String template = c.templateName();
		if (unit.findModel(template).isEmpty() && !broken.contains(template)
				&& !neverBuilt.contains(template)) {
			ctx.error(DiagnosticCodes.UNKNOWN_MODEL, loc,
					UNKNOWN_MODEL_MSG + template + "'");
		}
		// Otherwise the template never completes, because of a failure
		// reported where it is.
	}

	/**
	 * The models of a cycle of stuck commands that starts at {@code c}'s model,
	 * in order, or nothing when what it waits on never comes back to it. A
	 * model waits on its template, and the result of an operator call on its
	 * sources: a cycle may go through both (#183).
	 */
	private static List<String> cycleThrough(ImplementsTemplate c,
			List<Command> stuck) {
		List<String> path = new ArrayList<>(List.of(c.modelName()));
		return extend(path, c.templateName(), stuck, new HashSet<>())
				? path
				: List.of();
	}

	/**
	 * Whether {@code path}, followed by {@code next}, leads back to its first
	 * model through what each stuck model waits on; {@code path} then holds the
	 * cycle.
	 */
	private static boolean extend(List<String> path, String next,
			List<Command> stuck, Set<String> visited) {
		if (next.equals(path.getFirst())) {
			return true;
		}
		if (!visited.add(next)) {
			return false;
		}
		path.add(next);
		for (String after : dependencies(next, stuck)) {
			if (extend(path, after, stuck, visited)) {
				return true;
			}
		}
		path.removeLast();
		return false;
	}

	/**
	 * The models the stuck commands building {@code model} wait on: what each
	 * stuck command that targets it reads.
	 */
	private static List<String> dependencies(String model,
			List<Command> stuck) {
		return stuck.stream()
				.filter(cmd -> cmd.target().filter(model::equals).isPresent())
				.flatMap(cmd -> cmd.reads().stream()).toList();
	}

	/**
	 * Whether {@code c} reports its cycle: a cycle is reported once, by the
	 * {@code implements} of its first model in alphabetical order that has one.
	 * Operator calls with several sources can tie cycles into one knot: the
	 * knot's first model always reports one of them, and another may show only
	 * once that one is broken.
	 */
	private static boolean reportsCycle(ImplementsTemplate c,
			List<String> cycle, List<Command> stuck) {
		Set<String> implementing = stuckImplementations(stuck);
		return cycle.stream().filter(implementing::contains).sorted()
				.findFirst().orElseThrow().equals(c.modelName());
	}

	/**
	 * A cycle as a sentence: {@code a implements b implements a}, and, through
	 * an operator call, {@code j implements t, which 'assemble' builds from
	 * j}.
	 */
	private static String describeCycle(List<String> cycle,
			List<Command> stuck) {
		StringBuilder text = new StringBuilder(cycle.getFirst());
		for (int i = 0; i < cycle.size(); i++) {
			String model = cycle.get(i);
			String next = cycle.get((i + 1) % cycle.size());
			text.append(builtBy(model, stuck)
					.map(op -> ", which '" + op.operatorName()
							+ "' builds from " + next)
					.orElse(" implements " + next));
		}
		return text.toString();
	}

	/** The stuck operator call that builds {@code model}, if any. */
	private static Optional<ApplyOperator> builtBy(String model,
			List<Command> stuck) {
		return stuck.stream().<ApplyOperator>mapMulti((cmd, out) -> {
			if (cmd instanceof ApplyOperator a
					&& a.resultName().equals(model)) {
				out.accept(a);
			}
		}).findFirst();
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
