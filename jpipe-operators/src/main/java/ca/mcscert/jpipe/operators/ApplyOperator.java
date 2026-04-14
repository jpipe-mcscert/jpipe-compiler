package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.MacroCommand;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * A deferred {@link MacroCommand} that expands an operator call into the
 * {@link Command}s needed to build the result model.
 *
 * <p>
 * Execution is deferred until all source models named in {@link #sourceNames}
 * are present in the {@link Unit}. Once the condition is satisfied,
 * {@link #expand} looks up the operator in the {@link OperatorRegistry},
 * gathers the source models, and delegates to
 * {@link CompositionOperator#apply}.
 */
public final class ApplyOperator implements MacroCommand {

	private static final UnificationEquivalenceRegistry DEFAULT_UNIFICATION;

	static {
		DEFAULT_UNIFICATION = new UnificationEquivalenceRegistry();
		DEFAULT_UNIFICATION.register(Unifier.DEFAULT_UNIFY_BY,
				new ca.mcscert.jpipe.operators.equivalences.SameLabel());
	}

	private final String resultName;
	private final String operatorName;
	private final List<String> sourceNames;
	private final Map<String, String> arguments;
	private final OperatorRegistry operators;
	private final SourceLocation location;
	private final ModelKind declaredKind;
	private final UnificationEquivalenceRegistry unificationEquivalences;

	public ApplyOperator(String resultName, String operatorName,
			List<String> sourceNames, Map<String, String> arguments,
			OperatorRegistry operators) {
		this(resultName, operatorName, sourceNames, arguments, operators,
				SourceLocation.UNKNOWN, ModelKind.JUSTIFICATION,
				DEFAULT_UNIFICATION);
	}

	public ApplyOperator(String resultName, String operatorName,
			List<String> sourceNames, Map<String, String> arguments,
			OperatorRegistry operators, SourceLocation location) {
		this(resultName, operatorName, sourceNames, arguments, operators,
				location, ModelKind.JUSTIFICATION, DEFAULT_UNIFICATION);
	}

	public ApplyOperator(String resultName, String operatorName,
			List<String> sourceNames, Map<String, String> arguments,
			OperatorRegistry operators, SourceLocation location,
			ModelKind declaredKind) {
		this(resultName, operatorName, sourceNames, arguments, operators,
				location, declaredKind, DEFAULT_UNIFICATION);
	}

	public ApplyOperator(String resultName, String operatorName,
			List<String> sourceNames, Map<String, String> arguments,
			OperatorRegistry operators, SourceLocation location,
			ModelKind declaredKind,
			UnificationEquivalenceRegistry unificationEquivalences) {
		this.resultName = resultName;
		this.operatorName = operatorName;
		this.sourceNames = List.copyOf(sourceNames);
		this.arguments = Map.copyOf(arguments);
		this.operators = operators;
		this.location = location;
		this.declaredKind = declaredKind;
		this.unificationEquivalences = unificationEquivalences;
	}

	public String resultName() {
		return resultName;
	}

	public String operatorName() {
		return operatorName;
	}

	public List<String> sourceNames() {
		return sourceNames;
	}

	public Map<String, String> arguments() {
		return arguments;
	}

	public SourceLocation location() {
		return location;
	}

	@Override
	public Predicate<Unit> condition() {
		return unit -> sourceNames.stream()
				.allMatch(name -> unit.findModel(name).isPresent());
	}

	@Override
	public List<Command> expand(Unit context) throws Exception {
		CompositionOperator op = operators.find(operatorName).orElseThrow(
				() -> new InvalidOperatorCallException("Unknown operator: '"
						+ operatorName + "' at " + location));
		List<JustificationModel<?>> sources = sourceNames.stream()
				.<JustificationModel<?>>map(context::get).toList();
		ModelKind actualKind = op.resultKind(sources, arguments);
		if (actualKind != declaredKind) {
			throw new InvalidOperatorCallException(
					"[execution-error] operator '" + operatorName
							+ "' produces a " + actualKind.name().toLowerCase()
							+ " but was declared as '"
							+ declaredKind.name().toLowerCase() + "'");
		}
		List<Command> composed = op.apply(resultName, sources, arguments,
				location, context.locations());
		return new Unifier(unificationEquivalences).unify(resultName, composed,
				arguments);
	}

	@Override
	public String toString() {
		return "applyOperator('" + resultName + "', '" + operatorName + "', "
				+ sourceNames + ").";
	}
}
