package ca.mcscert.jpipe.model.validation;

import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.Violation;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Checks structural invariants of justification models.
 *
 * <p>
 * Rules:
 * <ul>
 * <li>{@code no-duplicate-ids} — all element IDs within a model are unique.
 * <li>{@code unique-identifiers}: no identifier an exported model can be
 * addressed by designates two different elements. This covers the merge aliases
 * as well as the ids: an alias that collides with another element's id would
 * make a reference ambiguous.
 * <li>{@code acyclic-support} — the support graph contains no cycles (it must
 * be a DAG).
 * <li>{@code acyclic-implements} — the implements chain between models contains
 * no cycles.
 * </ul>
 *
 * <p>
 * Note: type constraints (Conclusion ← Strategy ← SupportLeaf) are enforced by
 * the Java type system at construction time and need no runtime check here.
 *
 * <p>
 * Use {@link #validate(Unit)} for location-aware validation within a compiler
 * pipeline. Use {@link #validateModel(JustificationModel)} for standalone use
 * when no {@link Unit} is available; violations will carry
 * {@link SourceLocation#UNKNOWN}.
 */
public final class ConsistencyValidator {

	private static final String IN_MODEL = "' in model '";

	/**
	 * Validates all models in the unit. Violations carry source locations
	 * resolved from the unit's location registry.
	 */
	public List<Violation> validate(Unit unit) {
		ValidationContext ctx = ValidationContext.of(unit);
		List<Violation> violations = new ArrayList<>();
		for (JustificationModel<?> model : unit.getModels()) {
			violations.addAll(checkNoDuplicateIds(model, ctx));
			violations.addAll(checkUniqueIdentifiers(model, ctx));
			violations.addAll(checkAcyclicSupport(model, ctx));
		}
		violations.addAll(checkAcyclicImplements(unit, ctx));
		return violations;
	}

	/**
	 * Validates a single model without location data. All violations carry
	 * {@link SourceLocation#UNKNOWN}.
	 */
	public List<Violation> validateModel(JustificationModel<?> model) {
		ValidationContext ctx = ValidationContext.STANDALONE;
		List<Violation> violations = new ArrayList<>();
		violations.addAll(checkNoDuplicateIds(model, ctx));
		violations.addAll(checkUniqueIdentifiers(model, ctx));
		violations.addAll(checkAcyclicSupport(model, ctx));
		violations.addAll(checkAcyclicImplements(model));
		return violations;
	}

	// -------------------------------------------------------------------------

	private List<Violation> checkNoDuplicateIds(JustificationModel<?> model,
			ValidationContext ctx) {
		List<Violation> violations = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		model.conclusion().ifPresent(c -> {
			if (!seen.add(c.id())) {
				violations.add(new Violation("no-duplicate-ids",
						"Duplicate element id '" + c.id() + IN_MODEL
								+ model.getName() + "'",
						ctx.locationOf(model.getName(), c.id())));
			}
		});
		model.getElements().forEach(element -> {
			if (!seen.add(element.id())) {
				violations.add(new Violation("no-duplicate-ids",
						"Duplicate element id '" + element.id() + IN_MODEL
								+ model.getName() + "'",
						ctx.locationOf(model.getName(), element.id())));
			}
		});
		return violations;
	}

	/**
	 * Checks that no identifier designates two different elements.
	 *
	 * <p>
	 * An exported model can be addressed by more than an element's own id:
	 * every id merged into an element addresses it too, so that a reference
	 * written before a composition keeps working afterwards. Consumers
	 * therefore index ids and merge aliases into one namespace, and a key
	 * landing on two elements leaves them no way to choose. jpipe-runner
	 * rejects the whole model rather than guess. Element ids alone are covered
	 * by {@code no-duplicate-ids}; what this adds is the aliases.
	 */
	private List<Violation> checkUniqueIdentifiers(JustificationModel<?> model,
			ValidationContext ctx) {
		Set<String> elementIds = elementIdsOf(model);
		Map<String, String> designated = new HashMap<>();
		elementIds.forEach(id -> designated.put(id, id));

		List<Violation> violations = new ArrayList<>();
		model.aliases().forEach((alias, target) -> {
			String canonical = elementDesignatedBy(model, elementIds, target);
			if (canonical == null) {
				// The chain leads to no element of this model, so the alias is
				// never exported and can collide with nothing.
				return;
			}
			String existing = designated.putIfAbsent(alias, canonical);
			if (existing != null && !existing.equals(canonical)) {
				violations.add(new Violation("unique-identifiers",
						"Identifier '" + alias + IN_MODEL + model.getName()
								+ "' designates both element '" + existing
								+ "' and element '" + canonical + "'",
						ctx.locationOf(model.getName(), alias)));
			}
		});
		return violations;
	}

	/** Element ids of {@code model}, conclusion included. */
	private static Set<String> elementIdsOf(JustificationModel<?> model) {
		Set<String> ids = new HashSet<>();
		model.conclusion().ifPresent(c -> ids.add(c.id()));
		model.getElements().forEach(element -> ids.add(element.id()));
		return ids;
	}

	/**
	 * Follows {@code id} through the alias map until it reaches an element of
	 * {@code model}, or {@code null} if it reaches none or loops.
	 */
	private static String elementDesignatedBy(JustificationModel<?> model,
			Set<String> elementIds, String id) {
		Set<String> visited = new HashSet<>();
		String current = id;
		while (visited.add(current)) {
			if (elementIds.contains(current)) {
				return current;
			}
			current = model.aliases().get(current);
			if (current == null) {
				return null;
			}
		}
		return null;
	}

	private List<Violation> checkAcyclicSupport(JustificationModel<?> model,
			ValidationContext ctx) {
		// Build a support-edge map: id → list of ids of the elements it is
		// supported by
		Map<String, List<String>> edges = new HashMap<>();
		model.conclusion().ifPresent(c -> c.getSupport().ifPresent(s -> edges
				.computeIfAbsent(c.id(), k -> new ArrayList<>()).add(s.id())));
		model.subConclusions().forEach(sc -> sc.getSupport()
				.ifPresent(s -> edges
						.computeIfAbsent(sc.id(), k -> new ArrayList<>())
						.add(s.id())));
		model.strategies()
				.forEach(s -> s.getSupports()
						.forEach(leaf -> edges
								.computeIfAbsent(s.id(), k -> new ArrayList<>())
								.add(((JustificationElement) leaf).id())));

		String modelName = model.getName();
		List<Violation> violations = new ArrayList<>();
		GraphCycles.detect(edges.keySet(), Function.identity(),
				node -> edges.getOrDefault(node, List.of()), new HashSet<>(),
				node -> violations.add(new Violation("acyclic-support",
						"Cycle in support graph at element '" + node + IN_MODEL
								+ modelName + "'",
						ctx.locationOf(modelName, node))));
		return violations;
	}

	private List<Violation> checkAcyclicImplements(Unit unit,
			ValidationContext ctx) {
		List<Violation> violations = new ArrayList<>();
		GraphCycles
				.detect(unit.getModels(), JustificationModel::getName,
						ConsistencyValidator::parentsOf, new HashSet<>(),
						m -> violations.add(new Violation("acyclic-implements",
								"Cycle in implements chain at model '"
										+ m.getName() + "'",
								ctx.locationOf(m.getName()))));
		return violations;
	}

	private List<Violation> checkAcyclicImplements(
			JustificationModel<?> model) {
		List<Violation> violations = new ArrayList<>();
		GraphCycles
				.detect(List.of(model), JustificationModel::getName,
						ConsistencyValidator::parentsOf, new HashSet<>(),
						m -> violations.add(new Violation("acyclic-implements",
								"Cycle in implements chain at model '"
										+ m.getName() + "'",
								SourceLocation.UNKNOWN)));
		return violations;
	}

	/**
	 * Returns the direct parent of {@code model} as a singleton list, or empty.
	 */
	private static List<JustificationModel<?>> parentsOf(
			JustificationModel<?> model) {
		List<JustificationModel<?>> result = new ArrayList<>(1);
		model.getParent().ifPresent(result::add);
		return result;
	}
}
