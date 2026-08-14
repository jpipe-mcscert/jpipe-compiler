package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.ElementCreationCommand;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.MarkUnified;
import ca.mcscert.jpipe.commands.linking.RegisterAlias;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Phase 4 post-processor for composition operators.
 *
 * <p>
 * After {@link CompositionOperator#apply} returns its command list, this class
 * inspects the element-creation commands, groups equivalent ones according to a
 * named {@link EquivalenceRelation} looked up in a
 * {@link UnificationEquivalenceRegistry}, and rewrites the command list so that
 * each equivalence class is represented by a single synthesized element whose
 * id is {@code "unified_N"} (N = 0-based counter per merged group). All
 * original member ids are aliased to the new id via {@link RegisterAlias}
 * commands, and {@link AddSupport} commands referencing removed ids are
 * rewritten accordingly. Each new id is also flagged via {@link MarkUnified},
 * since it names a group by a counter and so appears in no source file;
 * exporters that address a human audience name the element by its originals
 * instead.
 *
 * <p>
 * A group may legitimately mix element kinds — one source argued a claim while
 * another still asserts it — so the merged element is built from the group's
 * <em>dominant</em> member rather than from whichever member happens to come
 * first: a sub-conclusion subsumes an evidence, and among members of the same
 * kind the first one wins (it also provides the merged element's label and
 * source location). A group whose kinds are incomparable cannot be merged and
 * raises an {@link IncompatibleUnificationException}. This makes composition
 * operators commutative: the result no longer depends on the order the source
 * models were listed in.
 *
 * <p>
 * Note that for an equivalence relation that does not compare labels, the
 * merged element's <em>label</em> is still the dominant member's, hence still
 * order-dependent among members of the same kind. The only registered relation,
 * {@code sameLabel}, cannot exhibit that.
 *
 * <p>
 * Controlled by two optional config parameters:
 * <ul>
 * <li>{@code unifyBy} — name of the equivalence relation to use (default:
 * {@code "sameLabel"})</li>
 * <li>{@code unifyExclude} — comma-separated list of result-model element ids
 * that must NOT participate in unification (default: empty)</li>
 * </ul>
 *
 * <p>
 * If no merged groups are found the original command list is returned
 * unchanged.
 */
public final class Unifier {

	static final String UNIFIED_PREFIX = "unified_";
	static final String UNIFY_BY_KEY = "unifyBy";
	static final String UNIFY_EXCLUDE_KEY = "unifyExclude";
	static final String DEFAULT_UNIFY_BY = "sameLabel";

	private final UnificationEquivalenceRegistry registry;

	public Unifier(UnificationEquivalenceRegistry registry) {
		this.registry = registry;
	}

	/**
	 * Applies Phase 4 unification to {@code commands}.
	 *
	 * @param resultName
	 *            the name of the result model (used in {@link RegisterAlias}
	 *            commands)
	 * @param commands
	 *            the command list produced by Phases 1–3 of
	 *            {@link CompositionOperator#apply}
	 * @param args
	 *            the operator config map; {@code unifyBy} and
	 *            {@code unifyExclude} are consumed here
	 * @return a new, unmodifiable command list with merged elements and
	 *         rewritten edges, or {@code commands} itself if nothing was merged
	 * @throws InvalidOperatorCallException
	 *             if {@code unifyBy} names an unknown equivalence relation
	 */
	public List<Command> unify(String resultName, List<Command> commands,
			Map<String, String> args) {

		String equivName = args.getOrDefault(UNIFY_BY_KEY, DEFAULT_UNIFY_BY);
		EquivalenceRelation equiv = registry.find(equivName)
				.orElseThrow(() -> new InvalidOperatorCallException(
						"[execution-error] unknown unification method '"
								+ equivName + "'; registered: "
								+ registry.registeredNames()));

		Set<String> excluded = parseExcludeList(
				args.getOrDefault(UNIFY_EXCLUDE_KEY, ""));

		// Collect element (non-model) creation commands
		List<Command> elementCmds = commands.stream().filter(Unifier::isElement)
				.toList();

		// Gather non-excluded element commands — no wrapping needed
		List<ElementCreationCommand> candidates = elementCmds.stream()
				.filter(cmd -> !excluded.contains(idOf(cmd)))
				.map(ElementCreationCommand.class::cast).toList();

		// Partition by equivalence relation
		List<List<ElementCreationCommand>> groups = Partitions
				.partitionBy(candidates, equiv);

		// For every group with more than one member: alias each member id to
		// unified_N, and elect the command the merged element is built from.
		Map<String, String> phase4Aliases = new LinkedHashMap<>();
		Map<String, ElementCreationCommand> prototypes = new LinkedHashMap<>();
		int counter = 0;
		for (List<ElementCreationCommand> group : groups) {
			if (group.size() > 1) {
				String unifiedId = UNIFIED_PREFIX + counter++;
				prototypes.put(unifiedId, dominant(resultName, group));
				for (ElementCreationCommand ecc : group) {
					phase4Aliases.put(ecc.identifier(), unifiedId);
				}
			}
		}

		if (phase4Aliases.isEmpty()) {
			return List.copyOf(commands);
		}

		List<Command> result = rebuildCommands(resultName, commands,
				phase4Aliases, prototypes);
		phase4Aliases.forEach((oldId, newId) -> result
				.add(new RegisterAlias(resultName, oldId, newId)));
		prototypes.keySet()
				.forEach(id -> result.add(new MarkUnified(resultName, id)));
		return List.copyOf(result);
	}

	// ── Helpers
	// ─────────────────────────────────────────────────────────────────

	private static List<Command> rebuildCommands(String resultName,
			List<Command> commands, Map<String, String> phase4Aliases,
			Map<String, ElementCreationCommand> prototypes) {
		List<Command> result = new ArrayList<>();
		Set<String> insertedUnified = new LinkedHashSet<>();
		Set<String> seenEdges = new LinkedHashSet<>();
		for (Command cmd : commands) {
			if (isElement(cmd)) {
				appendElement(result, cmd, phase4Aliases, prototypes,
						insertedUnified);
			} else if (cmd instanceof AddSupport as) {
				appendEdge(result, resultName, as, phase4Aliases, seenEdges);
			} else {
				result.add(cmd);
			}
		}
		return result;
	}

	private static void appendElement(List<Command> result, Command cmd,
			Map<String, String> phase4Aliases,
			Map<String, ElementCreationCommand> prototypes,
			Set<String> insertedUnified) {
		String id = idOf(cmd);
		if (phase4Aliases.containsKey(id)) {
			String unifiedId = phase4Aliases.get(id);
			// The merged element takes the position of the first member met,
			// but is built from the group's dominant one.
			if (insertedUnified.add(unifiedId)) {
				result.add(prototypes.get(unifiedId).withId(unifiedId));
			}
		} else {
			result.add(cmd);
		}
	}

	/**
	 * Elects the command a merged group is synthesized from.
	 *
	 * <p>
	 * The elected command is the first member whose kind subsumes every other
	 * kind in the group; its label and location are the ones the merged element
	 * carries. A group whose kinds are incomparable cannot be merged into a
	 * single element and is rejected.
	 *
	 * @throws IncompatibleUnificationException
	 *             if no member subsumes all the others.
	 */
	private static ElementCreationCommand dominant(String resultName,
			List<ElementCreationCommand> group) {
		List<JustificationElement> elements = group.stream()
				.map(ElementCreationCommand::element).toList();
		int best = 0;
		for (int i = 1; i < elements.size(); i++) {
			// Strict upgrade only: among equals, the first member wins.
			if (!subsumes(elements.get(best), elements.get(i))) {
				best = i;
			}
		}
		JustificationElement winner = elements.get(best);
		for (JustificationElement element : elements) {
			if (!subsumes(winner, element)) {
				throw incompatible(resultName, group);
			}
		}
		return group.get(best);
	}

	/**
	 * Tells whether an element of {@code winner}'s kind can stand for one of
	 * {@code other}'s kind in a merged group.
	 *
	 * <p>
	 * A {@link SubConclusion} subsumes an {@link Evidence}: being both
	 * {@link ca.mcscert.jpipe.model.elements.StrategyBacked} and
	 * {@link ca.mcscert.jpipe.model.elements.SupportLeaf}, it supports whatever
	 * the evidence supported and can additionally carry the strategy that
	 * argues it. No other pair of distinct kinds is comparable: a conclusion is
	 * the model's single root, a strategy is the support rather than a
	 * supporter, and an {@link AbstractSupport} placeholder is discharged by an
	 * explicit override, never by a merge.
	 */
	private static boolean subsumes(JustificationElement winner,
			JustificationElement other) {
		return switch (winner) {
			case SubConclusion _ ->
				other instanceof SubConclusion || other instanceof Evidence;
			case Conclusion _ -> other instanceof Conclusion;
			case Strategy _ -> other instanceof Strategy;
			case Evidence _ -> other instanceof Evidence;
			case AbstractSupport _ -> other instanceof AbstractSupport;
		};
	}

	/**
	 * Builds the rejection for a group that mixes incomparable kinds, naming
	 * the first incomparable pair in a canonical order so that the message does
	 * not depend on the order the source models were listed in.
	 */
	private static IncompatibleUnificationException incompatible(
			String resultName, List<ElementCreationCommand> group) {
		List<ElementCreationCommand> sorted = group.stream()
				.sorted(Comparator.comparing(
						(ElementCreationCommand c) -> c.element().kind())
						.thenComparing(ElementCreationCommand::identifier))
				.toList();
		ElementCreationCommand first = sorted.get(0);
		ElementCreationCommand clashing = sorted.stream()
				.filter(c -> !subsumes(first.element(), c.element())
						&& !subsumes(c.element(), first.element()))
				.findFirst().orElse(sorted.get(sorted.size() - 1));
		return new IncompatibleUnificationException("cannot unify "
				+ describe(first) + " with " + describe(clashing)
				+ " in model '" + resultName
				+ "': these element kinds are incompatible."
				+ " Rename one of the labels, or keep the elements apart with "
				+ UNIFY_EXCLUDE_KEY + ".");
	}

	/** Renders an element command as {@code 'id' (kind, "label")}. */
	private static String describe(ElementCreationCommand cmd) {
		return "'" + cmd.identifier() + "' (" + cmd.element().kind() + ", \""
				+ cmd.label() + "\")";
	}

	private static void appendEdge(List<Command> result, String resultName,
			AddSupport as, Map<String, String> phase4Aliases,
			Set<String> seenEdges) {
		String newSupportable = resolve(as.supportableId(), phase4Aliases);
		String newSupporter = resolve(as.supporterId(), phase4Aliases);
		String key = newSupportable + "->" + newSupporter;
		if (seenEdges.add(key)) {
			result.add(
					new AddSupport(resultName, newSupportable, newSupporter));
		}
	}

	private static Set<String> parseExcludeList(String raw) {
		if (raw.isBlank()) {
			return Set.of();
		}
		return Set.of(raw.split(",")).stream().map(String::trim)
				.collect(Collectors.toUnmodifiableSet());
	}

	/**
	 * Returns true for commands that create a model element (not a model
	 * itself).
	 */
	static boolean isElement(Command cmd) {
		return cmd instanceof ElementCreationCommand;
	}

	/** Extracts the element id from an element-creation command. */
	static String idOf(Command cmd) {
		return ((ElementCreationCommand) cmd).identifier();
	}

	/** Extracts the label from an element-creation command. */
	static String labelOf(Command cmd) {
		return ((ElementCreationCommand) cmd).label();
	}

	private static String resolve(String id, Map<String, String> aliases) {
		return aliases.getOrDefault(id, id);
	}
}
