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

/**
 * Checks structural invariants of justification models.
 *
 * <p>
 * Rules:
 * <ul>
 * <li>{@code no-duplicate-ids} — all element IDs within a model are unique.
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

	/**
	 * Validates all models in the unit. Violations carry source locations
	 * resolved from the unit's location registry.
	 */
	public List<Violation> validate(Unit unit) {
		List<Violation> violations = new ArrayList<>();
		for (JustificationModel<?> model : unit.getModels()) {
			violations.addAll(checkNoDuplicateIds(model, unit));
			violations.addAll(checkAcyclicSupport(model, unit));
		}
		violations.addAll(checkAcyclicImplements(unit));
		return violations;
	}

	/**
	 * Validates a single model without location data. All violations carry
	 * {@link SourceLocation#UNKNOWN}.
	 */
	public List<Violation> validateModel(JustificationModel<?> model) {
		List<Violation> violations = new ArrayList<>();
		violations.addAll(checkNoDuplicateIds(model, null));
		violations.addAll(checkAcyclicSupport(model, null));
		violations.addAll(checkAcyclicImplements(model));
		return violations;
	}

	// -------------------------------------------------------------------------

	private List<Violation> checkNoDuplicateIds(JustificationModel<?> model,
			Unit unit) {
		List<Violation> violations = new ArrayList<>();
		Set<String> seen = new HashSet<>();
		model.conclusion().ifPresent(c -> {
			if (!seen.add(c.id())) {
				violations.add(new Violation("no-duplicate-ids",
						"Duplicate element id '" + c.id() + "' in model '"
								+ model.getName() + "'",
						location(unit, model.getName(), c.id())));
			}
		});
		model.getElements().forEach(element -> {
			if (!seen.add(element.id())) {
				violations.add(new Violation("no-duplicate-ids",
						"Duplicate element id '" + element.id() + "' in model '"
								+ model.getName() + "'",
						location(unit, model.getName(), element.id())));
			}
		});
		return violations;
	}

	private List<Violation> checkAcyclicSupport(JustificationModel<?> model,
			Unit unit) {
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

		List<Violation> violations = new ArrayList<>();
		Set<String> globalVisited = new HashSet<>();
		for (String start : edges.keySet()) {
			if (!globalVisited.contains(start)) {
				detectCycle(start, edges, globalVisited, new HashSet<>(),
						model.getName(), unit, violations);
			}
		}
		return violations;
	}

	private void detectCycle(String node, Map<String, List<String>> edges,
			Set<String> globalVisited, Set<String> currentPath,
			String modelName, Unit unit, List<Violation> violations) {
		if (currentPath.contains(node)) {
			violations.add(new Violation("acyclic-support",
					"Cycle in support graph at element '" + node
							+ "' in model '" + modelName + "'",
					location(unit, modelName, node)));
			return;
		}
		if (globalVisited.contains(node)) {
			return;
		}
		globalVisited.add(node);
		currentPath.add(node);
		List<String> nexts = edges.getOrDefault(node, List.of());
		nexts.forEach(next -> detectCycle(next, edges, globalVisited,
				currentPath, modelName, unit, violations));
		currentPath.remove(node);
	}

	private List<Violation> checkAcyclicImplements(Unit unit) {
		List<Violation> violations = new ArrayList<>();
		Set<String> globalVisited = new HashSet<>();
		for (JustificationModel<?> model : unit.getModels()) {
			if (!globalVisited.contains(model.getName())) {
				walkImplementsChain(model, globalVisited, new HashSet<>(), unit,
						violations);
			}
		}
		return violations;
	}

	private void walkImplementsChain(JustificationModel<?> model,
			Set<String> globalVisited, Set<String> currentChain, Unit unit,
			List<Violation> violations) {
		String name = model.getName();
		if (currentChain.contains(name)) {
			violations.add(new Violation("acyclic-implements",
					"Cycle in implements chain at model '" + name + "'",
					unit.locationOf(name)));
			return;
		}
		if (globalVisited.contains(name)) {
			return;
		}
		globalVisited.add(name);
		currentChain.add(name);
		model.getParent().ifPresent(parent -> walkImplementsChain(parent,
				globalVisited, currentChain, unit, violations));
		currentChain.remove(name);
	}

	private List<Violation> checkAcyclicImplements(
			JustificationModel<?> model) {
		Set<String> seen = new HashSet<>();
		JustificationModel<?> current = model;
		while (current != null) {
			if (!seen.add(current.getName())) {
				return List.of(new Violation("acyclic-implements",
						"Cycle in implements chain at model '"
								+ current.getName() + "'",
						SourceLocation.UNKNOWN));
			}
			current = current.getParent().orElse(null);
		}
		return List.of();
	}

	private SourceLocation location(Unit unit, String modelName,
			String elementId) {
		if (unit == null) {
			return SourceLocation.UNKNOWN;
		}
		return unit.locationOf(modelName, elementId);
	}
}
