package ca.mcscert.jpipe.operators;

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
public final class UnificationEquivalenceRegistry
		extends
			Registry<EquivalenceRelation> {

	/** Registers {@code relation} under {@code name}. */
	public void register(String name, EquivalenceRelation relation) {
		put(name, relation);
	}
}
