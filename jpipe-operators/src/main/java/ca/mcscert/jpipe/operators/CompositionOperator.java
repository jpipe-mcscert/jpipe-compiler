package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.RegisterAlias;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
 * <li>{@link #createResultModel} — emits the command that declares the result
 * model</li>
 * </ol>
 *
 * <p>
 * Operators are stateless: the same instance can be reused for multiple
 * {@link #apply} calls with different arguments.
 */
public abstract class CompositionOperator {

	private final Logger logger = LogManager.getLogger(getClass());

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
	 * Returns the {@link ModelKind} of the model produced by this operator.
	 * Defaults to {@link ModelKind#JUSTIFICATION}. Override in subclasses that
	 * produce a {@code Template}.
	 */
	public ModelKind resultKind() {
		return ModelKind.JUSTIFICATION;
	}

	/**
	 * Returns the equivalence relation to use for partitioning elements from
	 * {@code sources} and {@code arguments}.
	 */
	protected abstract EquivalenceRelation equivalenceRelation(
			List<JustificationModel<?>> sources, Map<String, String> arguments);

	/**
	 * Returns the merge function to use for creating result elements from
	 * {@code sources} and {@code arguments}.
	 */
	protected abstract MergeFunction mergeFunction(
			List<JustificationModel<?>> sources, Map<String, String> arguments);

	/**
	 * Returns the command that creates the result model (typically
	 * {@code CreateJustification} or {@code CreateTemplate}).
	 *
	 * @param location
	 *            the source location of the operator call; forwarded to the
	 *            creation command so the result model appears in the symbol
	 *            table.
	 * @param sources
	 *            the source models being composed; provided so operators can
	 *            choose the result model type (e.g. {@code Template} vs
	 *            {@code Justification}) based on the source types.
	 */
	protected abstract Command createResultModel(String name,
			SourceLocation location, List<JustificationModel<?>> sources,
			Map<String, String> arguments);

	/**
	 * Returns additional commands to append after Phase 2 edge reconstruction.
	 * Override to inject synthesized elements and their edges that have no
	 * counterpart in any source model. Default: empty.
	 */
	protected List<Command> additionalCommands(String resultName,
			List<JustificationModel<?>> sources, AliasRegistry aliases,
			Map<String, String> args) {
		return List.of();
	}

	// ── Template Method
	// ────────────────────────────────────────────────────────

	/**
	 * Applies this operator to {@code sources} and returns the complete list of
	 * commands needed to build the result model named {@code resultName}. Uses
	 * {@link SourceLocation#UNKNOWN} for the result model's location.
	 *
	 * @throws InvalidOperatorCallException
	 *             if any key declared by {@link #requiredArguments()} is absent
	 *             from {@code arguments}
	 */
	public final List<Command> apply(String resultName,
			List<JustificationModel<?>> sources,
			Map<String, String> arguments) {
		return apply(resultName, sources, arguments, SourceLocation.UNKNOWN);
	}

	/**
	 * Applies this operator to {@code sources} and returns the complete list of
	 * commands needed to build the result model named {@code resultName}.
	 *
	 * @param location
	 *            source location of the operator call; forwarded to
	 *            {@link #createResultModel} so the result model is registered
	 *            in the symbol table.
	 * @throws InvalidOperatorCallException
	 *             if any key declared by {@link #requiredArguments()} is absent
	 *             from {@code arguments}
	 */
	public final List<Command> apply(String resultName,
			List<JustificationModel<?>> sources, Map<String, String> arguments,
			SourceLocation location) {
		return apply(resultName, sources, arguments, location, Map.of());
	}

	/**
	 * Applies this operator to {@code sources} and returns the complete list of
	 * commands needed to build the result model named {@code resultName}.
	 *
	 * @param location
	 *            source location of the operator call; forwarded to
	 *            {@link #createResultModel} so the result model is registered
	 *            in the symbol table.
	 * @param knownLocations
	 *            location registry of the compilation unit (keyed as
	 *            {@code "modelName/elementId"}); used to attach each source
	 *            element's original location to its copy in the result model.
	 * @throws InvalidOperatorCallException
	 *             if any key declared by {@link #requiredArguments()} is absent
	 *             from {@code arguments}
	 */
	public final List<Command> apply(String resultName,
			List<JustificationModel<?>> sources, Map<String, String> arguments,
			SourceLocation location,
			Map<String, SourceLocation> knownLocations) {

		Map<String, String> args = Map.copyOf(arguments);

		// Validate required arguments before doing any work
		List<String> missing = requiredArguments().stream()
				.filter(key -> !args.containsKey(key)).sorted().toList();
		if (!missing.isEmpty()) {
			throw new InvalidOperatorCallException(
					getClass().getSimpleName() + " requires argument(s) "
							+ missing + " but they were not provided");
		}

		// Collect all sourced elements from every source model, attaching each
		// element's original location from knownLocations when available.
		List<SourcedElement> all = new ArrayList<>();
		for (JustificationModel<?> source : sources) {
			source.conclusion()
					.ifPresent(c -> all.add(new SourcedElement(c, source,
							knownLocations.getOrDefault(
									source.getName() + "/" + c.id(),
									SourceLocation.UNKNOWN))));
			source.getElements()
					.forEach(e -> all.add(new SourcedElement(e, source,
							knownLocations.getOrDefault(
									source.getName() + "/" + e.id(),
									SourceLocation.UNKNOWN))));
		}

		// Partition into equivalence classes
		List<ElementGroup> groups = partition(all,
				equivalenceRelation(sources, args));
		logger.debug("{} element(s) partitioned into {} group(s)", all.size(),
				groups.size());

		// Phase 1: element creation — merge function populates alias registry
		AliasRegistry aliases = new AliasRegistry();
		List<Command> commands = new ArrayList<>();
		commands.add(createResultModel(resultName, location, sources, args));
		MergeFunction merge = mergeFunction(sources, args);
		for (ElementGroup group : groups) {
			logger.debug("Merging group of {} member(s)",
					group.members().size());
			commands.addAll(merge.merge(resultName, group, aliases));
		}

		// Persist aliases to Unit via RegisterAlias commands
		aliases.aliases().forEach((oldId, newId) -> commands
				.add(new RegisterAlias(resultName, oldId, newId)));

		// Phase 2: link reconstruction — translate original edges through
		// aliases using qualified ids (source.getName():element.id()).
		// Non-merged elements resolve to their source-prefixed id unchanged;
		// merged elements resolve through the registry to their new id.
		// Deduplication via seenEdges prevents duplicate AddSupport when
		// multiple source models share the same (post-alias) edge.
		Set<String> seenEdges = new LinkedHashSet<>();
		for (JustificationModel<?> source : sources) {
			source.conclusion()
					.ifPresent(c -> c.getSupport()
							.ifPresent(s -> addEdge(commands, seenEdges,
									resultName, aliases, source, c.id(),
									s.id())));
			source.subConclusions()
					.forEach(sc -> sc.getSupport()
							.ifPresent(s -> addEdge(commands, seenEdges,
									resultName, aliases, source, sc.id(),
									s.id())));
			source.strategies().forEach(s -> s.getSupports().forEach(leaf -> {
				String leafId = ((JustificationElement) leaf).id();
				addEdge(commands, seenEdges, resultName, aliases, source,
						s.id(), leafId);
			}));
		}

		// Phase 3: operator-specific synthesized elements and edges
		commands.addAll(additionalCommands(resultName, sources, aliases, args));

		return List.copyOf(commands);
	}

	// ── Helpers
	// ───────────────────────────────────────────────────────────────

	private static void addEdge(List<Command> commands, Set<String> seen,
			String resultName, AliasRegistry aliases,
			JustificationModel<?> source, String supportableId,
			String supporterId) {
		String newSupportable = aliases.resolve(qualId(source, supportableId));
		String newSupporter = aliases.resolve(qualId(source, supporterId));
		String key = newSupportable + "->" + newSupporter;
		if (seen.add(key)) {
			commands.add(
					new AddSupport(resultName, newSupportable, newSupporter));
		}
	}

	private static String qualId(JustificationModel<?> source, String id) {
		return id.contains(":") ? id : source.getName() + ":" + id;
	}

	private static List<ElementGroup> partition(List<SourcedElement> elements,
			EquivalenceRelation rel) {
		List<List<SourcedElement>> partitions = new ArrayList<>();
		for (SourcedElement candidate : elements) {
			boolean placed = false;
			for (List<SourcedElement> partition : partitions) {
				if (rel.areEquivalent(partition.get(0), candidate)) {
					partition.add(candidate);
					placed = true;
					break;
				}
			}
			if (!placed) {
				List<SourcedElement> fresh = new ArrayList<>();
				fresh.add(candidate);
				partitions.add(fresh);
			}
		}
		return partitions.stream().map(ElementGroup::new).toList();
	}
}
