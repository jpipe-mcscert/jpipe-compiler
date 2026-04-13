package ca.mcscert.jpipe.operators;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Name-to-{@link EquivalenceRelation} registry used by {@link Unifier} for the
 * automatic post-composition unification phase.
 *
 * <p>
 * Populated at compiler startup with built-in relations; looked up at
 * model-build time via the {@code unifyBy} config parameter.
 * {@code SameShortId} is intentionally not registered here — it is reserved for
 * Phase 1 operator equivalence only.
 */
public final class UnificationEquivalenceRegistry {

	private final Map<String, EquivalenceRelation> relations = new LinkedHashMap<>();

	/** Registers {@code relation} under {@code name}. */
	public void register(String name, EquivalenceRelation relation) {
		relations.put(name, relation);
	}

	/**
	 * Returns the relation registered under {@code name}, or empty if none was
	 * registered.
	 */
	public Optional<EquivalenceRelation> find(String name) {
		return Optional.ofNullable(relations.get(name));
	}

	/** Read-only view of all registered relation names. */
	public Set<String> registeredNames() {
		return Collections.unmodifiableSet(relations.keySet());
	}
}
