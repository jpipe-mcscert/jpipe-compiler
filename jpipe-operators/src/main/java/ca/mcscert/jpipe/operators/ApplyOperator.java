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

	private final String resultName;
	private final String operatorName;
	private final List<String> sourceNames;
	private final Map<String, String> arguments;
	private final OperatorRegistry operators;
	private final SourceLocation location;
	private final ModelKind declaredKind;

	public ApplyOperator(String resultName, String operatorName,
			List<String> sourceNames, Map<String, String> arguments,
			OperatorRegistry operators) {
		this(resultName, operatorName, sourceNames, arguments, operators,
				SourceLocation.UNKNOWN, ModelKind.JUSTIFICATION);
	}

	public ApplyOperator(String resultName, String operatorName,
			List<String> sourceNames, Map<String, String> arguments,
			OperatorRegistry operators, SourceLocation location) {
		this(resultName, operatorName, sourceNames, arguments, operators,
				location, ModelKind.JUSTIFICATION);
	}

	public ApplyOperator(String resultName, String operatorName,
			List<String> sourceNames, Map<String, String> arguments,
			OperatorRegistry operators, SourceLocation location,
			ModelKind declaredKind) {
		this.resultName = resultName;
		this.operatorName = operatorName;
		this.sourceNames = List.copyOf(sourceNames);
		this.arguments = Map.copyOf(arguments);
		this.operators = operators;
		this.location = location;
		this.declaredKind = declaredKind;
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
		if (op.resultKind() != declaredKind) {
			throw new InvalidOperatorCallException(
					"[execution-error] operator '" + operatorName
							+ "' produces a "
							+ op.resultKind().name().toLowerCase()
							+ " but was declared as '"
							+ declaredKind.name().toLowerCase() + "'");
		}
		List<JustificationModel<?>> sources = sourceNames.stream()
				.<JustificationModel<?>>map(context::get).toList();
		return op.apply(resultName, sources, arguments, location,
				context.locations());
	}

	@Override
	public String toString() {
		return "applyOperator('" + resultName + "', '" + operatorName + "', "
				+ sourceNames + ").";
	}
}
