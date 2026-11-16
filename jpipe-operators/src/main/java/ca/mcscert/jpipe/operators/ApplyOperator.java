package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.MacroCommand;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.Optional;
import ca.mcscert.jpipe.commands.PendingCommands;
import java.util.function.UnaryOperator;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.stream.Collectors;

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

	/**
	 * This call with every model name it refers to, its result and its sources,
	 * passed through {@code rename}, and every element id its arguments hold
	 * (see {@link CompositionOperator#elementArguments()} and
	 * {@code unifyExclude}) through {@code renameElement}: how a file loaded
	 * under a namespace refers to its own models and elements (#178).
	 */
	public ApplyOperator withModelNames(UnaryOperator<String> rename,
			UnaryOperator<String> renameElement) {
		Set<String> elementKeys = operators.find(config.operatorName())
				.map(CompositionOperator::elementArguments).orElse(Set.of());
		Map<String, String> arguments = new LinkedHashMap<>();
		config.arguments().forEach((key, value) -> {
			String renamed = value;
			if (elementKeys.contains(key)) {
				renamed = renameElement.apply(value.strip());
			} else if (key.equals(Unifier.UNIFY_EXCLUDE_KEY)) {
				renamed = Arrays.stream(value.split(",")).map(String::strip)
						.map(renameElement).collect(Collectors.joining(","));
			}
			arguments.put(key, renamed);
		});
		OperatorCallConfig renamedConfig = new OperatorCallConfig(
				rename.apply(config.resultName()), config.operatorName(),
				config.sourceNames().stream().map(rename).toList(), arguments,
				config.location(), config.declaredKind());
		return new ApplyOperator(renamedConfig, operators,
				unificationEquivalences);
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

	@Override
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
						"unknown operator '" + config.operatorName()
								+ "'; registered: "
								+ operators.registeredNames()));
		List<JustificationModel> sources = config.sourceNames().stream()
				.<JustificationModel>map(context::get).toList();
		ModelKind actualKind = op.resultKind(sources, config.arguments());
		if (actualKind != config.declaredKind()) {
			throw new InvalidOperatorCallException("operator '"
					+ config.operatorName() + "' produces a "
					+ actualKind.name().toLowerCase() + " but was declared as '"
					+ config.declaredKind().name().toLowerCase() + "'");
		}
		List<Command> composed = op.apply(config.resultName(), sources,
				config.arguments(), config.location(), context.locations(),
				context.aliases());
		return new Unifier(unificationEquivalences).unify(config.resultName(),
				composed, config.arguments());
	}

	/**
	 * Waits, beyond its sources existing, until they are complete: an operator
	 * composes what its source models are, not what they are so far (#177).
	 */
	@Override
	public boolean ready(Unit unit, PendingCommands pending) {
		return condition().test(unit)
				&& config.sourceNames().stream().noneMatch(pending::target)
				&& config.sourceNames().stream().allMatch(unit::isComplete);
	}

	/**
	 * When its sources exist and none failed to be built: a source still held
	 * back by a mistake of its own is composed as far as it got, so that the
	 * mistake is reported once, where it is, rather than as a result that is
	 * never built (#177).
	 */
	@Override
	public boolean runWhenStuck(Unit unit) {
		return condition().test(unit)
				&& config.sourceNames().stream().allMatch(unit::isComplete);
	}

	@Override
	public Optional<String> target() {
		return Optional.of(config.resultName());
	}

	@Override
	public String toString() {
		return "applyOperator('" + config.resultName() + "', '"
				+ config.operatorName() + "', " + config.sourceNames() + ").";
	}
}
