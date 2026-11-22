package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.util.QualifiedIds;
import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.MarkUnified;
import ca.mcscert.jpipe.commands.linking.RegisterAlias;
import ca.mcscert.jpipe.model.Composition.Scope;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * Base class for all composition operators. Owns the partition + two-phase
 * (create-then-link) algorithm via the Template Method pattern.
 *
 * <p>
 * Subclasses provide three hooks:
 * <ol>
 * <li>{@link #equivalenceRelation} — decides which elements belong to the same
 * group</li>
 * <li>{@link #mergeFunction} — creates the merged element for each group and
 * populates the {@link AliasRegistry}</li>
 * <li>{@link #resultKind} — chooses whether the result is a justification or a
 * template</li>
 * </ol>
 *
 * <p>
 * Operators are stateless: the same instance can be reused for multiple
 * {@link #apply} calls with different arguments.
 */
public abstract class CompositionOperator {

	private final Logger logger = LogManager.getLogger(getClass());

	/**
	 * Declares which argument keys this operator accepts without requiring
	 * them. Any key that is neither required, optional, nor one of the
	 * unification keys ({@code unifyBy}, {@code unifyExclude}) is refused, so
	 * that a misspelt key is an error instead of being ignored (#184). Default:
	 * none.
	 */
	protected Set<String> optionalArguments() {
		return Set.of();
	}

	/** Refuses the keys no part of the call reads: see optionalArguments. */
	private void requireKnownArguments(Map<String, String> args) {
		Set<String> accepted = new TreeSet<>(requiredArguments());
		accepted.addAll(optionalArguments());
		accepted.add(Unifier.UNIFY_BY_KEY);
		accepted.add(Unifier.UNIFY_EXCLUDE_KEY);
		List<String> unknown = args.keySet().stream()
				.filter(key -> !accepted.contains(key)).sorted().toList();
		if (!unknown.isEmpty()) {
			throw new InvalidOperatorCallException("unknown argument(s) "
					+ unknown + "; accepted: " + accepted);
		}
	}

	/**
	 * Refuses the same model given twice: composing a model with itself either
	 * merges every element with its copy or fails on an unrelated unification
	 * error (#181).
	 */
	private static void requireDistinctSources(
			List<JustificationModel> sources) {
		Set<String> seen = new HashSet<>();
		for (JustificationModel source : sources) {
			if (!seen.add(source.getName())) {
				throw new InvalidOperatorCallException("the model '"
						+ source.getName() + "' is given more than once");
			}
		}
	}

	/**
	 * Declares which argument keys of this operator hold element ids, and where
	 * those ids name an element: in a source, as that source names it, such as
	 * {@code refine}'s {@code hook}, or in the result. A call read from a file
	 * loaded under a namespace renames the qualified ids they hold, as it
	 * renames its source models, and so does the JPIPE export of the result.
	 * {@code unifyExclude} is declared by the framework (see
	 * {@link #idArgumentScopes}). Default: none.
	 */
	protected Map<String, Scope> idArguments() {
		return Map.of();
	}

	/**
	 * Every argument key of a call to this operator that holds element ids,
	 * with its scope: the ones the operator declares, and {@code unifyExclude},
	 * whose ids name an element of the result.
	 */
	public final Map<String, Scope> idArgumentScopes() {
		Map<String, Scope> scopes = new LinkedHashMap<>(idArguments());
		scopes.put(Unifier.UNIFY_EXCLUDE_KEY, Scope.RESULT);
		return Map.copyOf(scopes);
	}

	/**
	 * Declares which argument keys this operator requires. The framework
	 * validates their presence at the start of {@link #apply} and throws
	 * {@link InvalidOperatorCallException} listing any missing keys. Default:
	 * empty set (no required arguments).
	 */
	protected Set<String> requiredArguments() {
		return Set.of();
	}

	/**
	 * Returns the equivalence relation to use for partitioning elements from
	 * {@code sources} and {@code arguments}. An id renamed by a prior
	 * composition step is resolved through its source's
	 * {@link JustificationModel#resolveAlias aliases}.
	 */
	protected abstract EquivalenceRelation equivalenceRelation(
			List<JustificationModel> sources, Map<String, String> arguments);

	/**
	 * Returns the merge function to use for creating result elements from
	 * {@code sources} and {@code arguments}.
	 */
	protected abstract MergeFunction mergeFunction(
			List<JustificationModel> sources, Map<String, String> arguments);

	/**
	 * Returns the kind of model the result is, once the commands creating its
	 * elements are known, so that an operator whose result kind depends on what
	 * it produced (as {@code refine}'s does on whether a {@code @support}
	 * remains) can tell. The framework creates the result model of that kind.
	 *
	 * @param sources
	 *            the source models being composed, so that the kind can follow
	 *            theirs.
	 * @param elements
	 *            the commands creating the result's elements, in Phase 1.
	 */
	protected abstract ModelKind resultKind(List<JustificationModel> sources,
			Map<String, String> arguments, List<Command> elements);

	/**
	 * Returns additional commands to append after Phase 2 edge reconstruction.
	 * Override to inject synthesized elements and their edges that have no
	 * counterpart in any source model. Default: empty.
	 */
	@SuppressWarnings("java:S1172") // params unused in default but required by
									// overrides
	protected List<Command> additionalCommands(String resultName,
			List<JustificationModel> sources, AliasRegistry aliases,
			Map<String, String> args) {
		return List.of();
	}

	// ── Template Method
	// ────────────────────────────────────────────────────────

	/**
	 * Applies this operator to {@code sources} and returns the complete list of
	 * commands needed to build the result model named {@code resultName}.
	 *
	 * @param location
	 *            source location of the operator call; attached to the result
	 *            model's creation so that it is registered in the symbol table.
	 * @param knownLocations
	 *            location registry of the compilation unit (keyed as
	 *            {@code "modelName/elementId"}); used to attach each source
	 *            element's original location to its copy in the result model.
	 * @throws InvalidOperatorCallException
	 *             if any key declared by {@link #requiredArguments()} is absent
	 *             from {@code arguments}, if a key is neither required,
	 *             optional ({@link #optionalArguments()}) nor a unification
	 *             key, or if a source model is given more than once
	 */
	public final Composed apply(String resultName,
			List<JustificationModel> sources, Map<String, String> arguments,
			SourceLocation location,
			Map<String, SourceLocation> knownLocations) {

		Map<String, String> args = Map.copyOf(arguments);

		// Validate required arguments before doing any work
		List<String> missing = requiredArguments().stream()
				.filter(key -> !args.containsKey(key)).sorted().toList();
		if (!missing.isEmpty()) {
			throw new InvalidOperatorCallException(
					"missing required argument(s) " + missing);
		}
		requireKnownArguments(args);
		requireDistinctSources(sources);

		// Collect all sourced elements from every source model, attaching each
		// element's original location from knownLocations when available.
		List<SourcedElement> all = new ArrayList<>();
		for (JustificationModel source : sources) {
			for (JustificationElement e : source.allElements()) {
				all.add(new SourcedElement(e, source,
						knownLocations.getOrDefault(
								Unit.registryKey(source.getName(), e.id()),
								SourceLocation.UNKNOWN)));
			}
		}

		// Partition into equivalence classes
		List<ElementGroup> groups = Partitions.partition(all,
				equivalenceRelation(sources, args));
		logger.debug("{} element(s) partitioned into {} group(s)", all.size(),
				groups.size());

		// Phase 1: element creation — merge function populates alias registry
		AliasRegistry aliases = new AliasRegistry();
		List<Command> commands = new ArrayList<>();
		MergeFunction merge = mergeFunction(sources, args);
		List<Command> elements = new ArrayList<>();
		for (ElementGroup group : groups) {
			logger.debug("Merging group of {} member(s)",
					group.members().size());
			elements.addAll(merge.merge(resultName, group, aliases));
		}
		ModelKind kind = resultKind(sources, args, elements);
		commands.add(kind == ModelKind.TEMPLATE
				? new CreateTemplate(resultName, location)
				: new CreateJustification(resultName, location));
		commands.addAll(elements);

		// Carry forward transitive aliases from source models.
		// For each prior alias "oldId → intermId" of a source (recorded in a
		// previous composition step), resolve "sourceName:intermId" through the
		// current local registry to find the final id in this result, then
		// register "sourceName:oldId → finalId" so callers can still reach
		// the element under its pre-composition name.
		for (JustificationModel source : sources) {
			source.aliases().forEach((oldId, intermId) -> {
				String qualOldId = QualifiedIds.qualify(source.getName(),
						oldId);
				String finalId = aliases.resolve(
						QualifiedIds.qualify(source.getName(), intermId));
				if (!qualOldId.equals(finalId)) {
					aliases.register(finalId, qualOldId);
				}
			});
		}

		// Persist aliases to Unit via RegisterAlias commands
		aliases.aliases().forEach((oldId, newId) -> commands
				.add(new RegisterAlias(resultName, oldId, newId)));

		commands.addAll(carryForwardUnifiedIds(resultName, sources));

		// Phase 2: link reconstruction
		Set<String> seenEdges = new LinkedHashSet<>();
		for (JustificationModel source : sources) {
			source.forEachSupportEdge(
					(supported, supporter) -> addEdge(commands, seenEdges,
							resultName, aliases, source, supported, supporter));
		}

		// Phase 3: operator-specific synthesized elements and edges
		commands.addAll(additionalCommands(resultName, sources, aliases, args));

		return new Composed(kind, commands);
	}

	// ── Helpers
	// ───────────────────────────────────────────────────────────────

	private static void addEdge(List<Command> commands, Set<String> seen,
			String resultName, AliasRegistry aliases, JustificationModel source,
			String supportableId, String supporterId) {
		String newSupportable = aliases
				.resolve(QualifiedIds.qualify(source.getName(), supportableId));
		String newSupporter = aliases
				.resolve(QualifiedIds.qualify(source.getName(), supporterId));
		String key = newSupportable + "->" + newSupporter;
		if (seen.add(key)) {
			commands.add(
					new AddSupport(resultName, newSupportable, newSupporter));
		}
	}

	/**
	 * Carries forward which ids unification minted in the sources. A source
	 * composed earlier may contribute a {@code unified_N} element; qualified
	 * into this result it stays an id no author wrote, whether it survives as
	 * an element or lives on only as an alias of a further merge.
	 */
	private static List<Command> carryForwardUnifiedIds(String resultName,
			List<JustificationModel> sources) {
		return sources.stream().flatMap(source -> source.unifiedIds().stream()
				.map(unified -> (Command) new MarkUnified(resultName,
						QualifiedIds.qualify(source.getName(), unified))))
				.toList();
	}

}
