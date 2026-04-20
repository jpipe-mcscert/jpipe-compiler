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

	private final OperatorCallConfig config;
	private final OperatorRegistry operators;
	private final UnificationEquivalenceRegistry unificationEquivalences;

	public ApplyOperator(OperatorCallConfig config, OperatorRegistry operators,
			UnificationEquivalenceRegistry unificationEquivalences) {
		this.config = config;
		this.operators = operators;
		this.unificationEquivalences = unificationEquivalences;
	}

	public String resultName() {
		return config.resultName();
	}

	public String operatorName() {
		return config.operatorName();
	}

	public List<String> sourceNames() {
		return config.sourceNames();
	}

	public Map<String, String> arguments() {
		return config.arguments();
	}

	public SourceLocation location() {
		return config.location();
	}

	@Override
	public Predicate<Unit> condition() {
		return unit -> config.sourceNames().stream()
				.allMatch(name -> unit.findModel(name).isPresent());
	}

	@Override
	public List<Command> expand(Unit context) {
		CompositionOperator op = operators.find(config.operatorName())
				.orElseThrow(() -> new InvalidOperatorCallException(
						"Unknown operator: '" + config.operatorName() + "' at "
								+ config.location()));
		List<JustificationModel<?>> sources = config.sourceNames().stream()
				.<JustificationModel<?>>map(context::get).toList();
		ModelKind actualKind = op.resultKind(sources, config.arguments());
		if (actualKind != config.declaredKind()) {
			throw new InvalidOperatorCallException(
					"[execution-error] operator '" + config.operatorName()
							+ "' produces a " + actualKind.name().toLowerCase()
							+ " but was declared as '"
							+ config.declaredKind().name().toLowerCase() + "'");
		}
		List<Command> composed = op.apply(config.resultName(), sources,
				config.arguments(), config.location(), context.locations());
		return new Unifier(unificationEquivalences).unify(config.resultName(),
				composed, config.arguments());
	}

	@Override
	public String toString() {
		return "applyOperator('" + config.resultName() + "', '"
				+ config.operatorName() + "', " + config.sourceNames() + ").";
	}
}
