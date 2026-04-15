package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.ElementCreationCommand;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.RegisterAlias;
import java.util.ArrayList;
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
 * rewritten accordingly.
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
				.map(cmd -> (ElementCreationCommand) cmd).toList();

		// Partition by equivalence relation
		List<List<ElementCreationCommand>> groups = Partitions
				.partitionBy(candidates, equiv);

		// Build phase4Aliases: oldId → unified_N for every group with >1 member
		Map<String, String> phase4Aliases = new LinkedHashMap<>();
		int counter = 0;
		for (List<ElementCreationCommand> group : groups) {
			if (group.size() > 1) {
				String unifiedId = UNIFIED_PREFIX + counter++;
				for (ElementCreationCommand ecc : group) {
					phase4Aliases.put(ecc.identifier(), unifiedId);
				}
			}
		}

		if (phase4Aliases.isEmpty()) {
			return List.copyOf(commands);
		}

		// Rebuild command list
		List<Command> result = new ArrayList<>();
		Set<String> insertedUnified = new LinkedHashSet<>();
		Set<String> seenEdges = new LinkedHashSet<>();

		for (Command cmd : commands) {
			if (isElement(cmd)) {
				String id = idOf(cmd);
				if (phase4Aliases.containsKey(id)) {
					// Replace the FIRST occurrence with the synthesized element
					String unifiedId = phase4Aliases.get(id);
					if (insertedUnified.add(unifiedId)) {
						result.add(synthesized(cmd, unifiedId));
					}
					// All subsequent occurrences (other group members) are
					// dropped
				} else {
					result.add(cmd);
				}
			} else if (cmd instanceof AddSupport as) {
				String newSupportable = resolve(as.supportableId(),
						phase4Aliases);
				String newSupporter = resolve(as.supporterId(), phase4Aliases);
				String key = newSupportable + "->" + newSupporter;
				if (seenEdges.add(key)) {
					result.add(new AddSupport(resultName, newSupportable,
							newSupporter));
				}
			} else {
				result.add(cmd);
			}
		}

		// Append RegisterAlias commands for all phase4 merges
		phase4Aliases.forEach((oldId, newId) -> result
				.add(new RegisterAlias(resultName, oldId, newId)));

		return List.copyOf(result);
	}

	// ── Helpers
	// ─────────────────────────────────────────────────────────────────

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

	/**
	 * Returns a copy of {@code original} with {@code newId} as the element
	 * identifier, preserving the command type, container, label, and location.
	 */
	private static Command synthesized(Command original, String newId) {
		return ((ElementCreationCommand) original).withId(newId);
	}

	private static String resolve(String id, Map<String, String> aliases) {
		return aliases.getOrDefault(id, id);
	}
}
