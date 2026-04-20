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
