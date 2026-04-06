package ca.mcscert.jpipe.operators;

/**
 * Binary predicate that decides whether two {@link SourcedElement}s belong to
 * the same equivalence class and should therefore be merged.
 *
 * <p>
 * Implementations must be reflexive, symmetric, and transitive.
 * {@link CompositionOperator} applies this relation to compute the partition
 * automatically — callers do not need to drive the pairing loop themselves.
 */
@FunctionalInterface
public interface EquivalenceRelation {

	/**
	 * Returns {@code true} if {@code a} and {@code b} should be merged into the
	 * same result element.
	 */
	boolean areEquivalent(SourcedElement a, SourcedElement b);
}
