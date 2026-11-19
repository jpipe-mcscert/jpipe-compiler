package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.MacroCommand;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.QualifiedIdReadings;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.Optional;
import ca.mcscert.jpipe.commands.PendingCommands;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.RecordComposition;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import ca.mcscert.jpipe.util.QualifiedIds;
import java.util.ArrayList;

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
	/**
	 * The namespace the file of this call is loaded under, all enclosing ones
	 * included, or {@code ""}: the element ids its arguments hold are written
	 * as that file names them, and mapped onto the unit when the call runs.
	 */
	private final String namespace;

	public ApplyOperator(OperatorCallConfig config, OperatorRegistry operators,
			UnificationEquivalenceRegistry unificationEquivalences) {
		this(config, operators, unificationEquivalences, "");
	}

	private ApplyOperator(OperatorCallConfig config, OperatorRegistry operators,
			UnificationEquivalenceRegistry unificationEquivalences,
			String namespace) {
		this.config = config;
		this.operators = operators;
		this.unificationEquivalences = unificationEquivalences;
		this.namespace = namespace;
	}

	/**
	 * This call, read from a file loaded under {@code ns}: its result and
	 * sources are renamed into the namespace, and the namespace is recorded so
	 * that the element ids its arguments hold are mapped when the call runs
	 * (see {@link #inUnit}). Ids cannot be renamed as text here: whether a
	 * segment of {@code both:a:e} is a model name, and which, is known only
	 * from the models that exist (#178).
	 */
	public ApplyOperator withNamespace(String ns) {
		OperatorCallConfig renamed = new OperatorCallConfig(
				QualifiedIds.qualify(ns, config.resultName()),
				config.operatorName(),
				config.sourceNames().stream()
						.map(name -> QualifiedIds.qualify(ns, name)).toList(),
				config.arguments(), config.location(), config.declaredKind());
		return new ApplyOperator(renamed, operators, unificationEquivalences,
				namespace.isEmpty() ? ns : QualifiedIds.qualify(ns, namespace));
	}

	/**
	 * {@code id}, an argument of a call on {@code sources} written in a file
	 * loaded under {@code namespace}, as the unit names it. A plain id is local
	 * and unchanged. A qualified one is read by provenance through the call's
	 * sources, each named as the file names it, without the namespace, and
	 * written back with the unit's names: under {@code ns}, {@code both:a:e} is
	 * {@code ns:both:ns:a:e} when {@code both} composes {@code a} (#178). A
	 * segment names a model only when the rest of the id is an id of that
	 * model, so that {@code b:ns:a:e} is never read as {@code (b:ns)(a)(e)}
	 * when only {@code (b)(ns:a)(e)} names an element (#202).
	 */
	static String inUnit(String id, String namespace,
			List<JustificationModel> sources) {
		if (namespace.isEmpty()) {
			return id;
		}
		String prefix = namespace + ":";
		Function<JustificationModel, String> asWritten = m -> m.getName()
				.startsWith(prefix)
						? m.getName().substring(prefix.length())
						: m.getName();
		// Every reading writes the same unit id: each model name it holds gets
		// the same prefix. What differs between them is which models the id
		// passes through, and only an export renaming those models can tell.
		return QualifiedIdReadings.inCall(sources, id, asWritten).getFirst()
				.with(JustificationModel::getName);
	}

	/** The arguments, with the element ids they hold as the unit names them. */
	private Map<String, String> argumentsIn(List<JustificationModel> sources,
			CompositionOperator op) {
		if (namespace.isEmpty()) {
			return config.arguments();
		}
		Set<String> idKeys = idArguments(op);
		Map<String, String> arguments = new LinkedHashMap<>();
		config.arguments().forEach((key, value) -> arguments.put(key,
				idKeys.contains(key) ? idsIn(value, sources) : value));
		return arguments;
	}

	/** A comma-separated list of ids, each as the unit names it. */
	private String idsIn(String value, List<JustificationModel> sources) {
		return Arrays.stream(value.split(",")).map(String::strip)
				.map(id -> inUnit(id, namespace, sources))
				.collect(Collectors.joining(","));
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
		// Built first, then checked: a mistake in the call is reported as
		// itself, not as the kind mismatch it may cause, and the kind checked
		// is the one the composition actually produced.
		Map<String, String> arguments = argumentsIn(sources, op);
		List<Command> composed = op.apply(config.resultName(), sources,
				arguments, config.location(), context.locations(),
				context.aliases());
		ModelKind actualKind = composed.getFirst() instanceof CreateTemplate
				? ModelKind.TEMPLATE
				: ModelKind.JUSTIFICATION;
		if (actualKind != config.declaredKind()) {
			throw new InvalidOperatorCallException("operator '"
					+ config.operatorName() + "' produces a "
					+ actualKind.name().toLowerCase(Locale.ROOT)
					+ " but was declared as '"
					+ config.declaredKind().name().toLowerCase(Locale.ROOT)
					+ "'");
		}
		List<Command> commands = new ArrayList<>(
				new Unifier(unificationEquivalences).unify(config.resultName(),
						composed, arguments));
		commands.add(new RecordComposition(config.resultName(),
				config.operatorName(), config.sourceNames(), arguments,
				idArguments(op)));
		return commands;
	}

	/** The keys of a call to {@code op} whose value holds element ids. */
	private static Set<String> idArguments(CompositionOperator op) {
		Set<String> keys = new HashSet<>(op.elementArguments());
		keys.add(Unifier.UNIFY_EXCLUDE_KEY);
		return keys;
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
	 * When its sources exist, none failed to be built, and none still waits on
	 * its {@code implements}: a source held back by a mistake of its own is
	 * composed as far as it got, so that the mistake is reported once, where it
	 * is, rather than as a result that is never built (#177). A source that
	 * never got its inherited structure is no source to compose.
	 */
	@Override
	public boolean runWhenStuck(Unit unit, PendingCommands pending) {
		return condition().test(unit)
				&& config.sourceNames().stream().allMatch(unit::isComplete)
				&& config.sourceNames().stream()
						.noneMatch(source -> pending.target(source,
								c -> !(c instanceof ImplementsTemplate)));
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
