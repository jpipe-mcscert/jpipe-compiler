package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.model.elements.ElementView;

/**
 * Binary predicate that decides whether two {@link ElementView}s belong to the
 * same equivalence class and should therefore be merged.
 *
 * <p>
 * Implementations must be reflexive, symmetric, and transitive.
 * {@link CompositionOperator} applies this relation to compute the partition
 * automatically — callers do not need to drive the pairing loop themselves.
 *
 * <p>
 * The parameter type is {@link ElementView} rather than {@link SourcedElement}
 * so that the same relation can be applied both during Phase 1 composition
 * (where inputs are {@link SourcedElement}s) and during Phase 4 unification in
 * {@link Unifier} (where inputs are
 * {@link ca.mcscert.jpipe.commands.creation.ElementCreationCommand}s). Both
 * types implement {@link ElementView}.
 */
@FunctionalInterface
public interface EquivalenceRelation {

	/**
	 * Returns {@code true} if {@code a} and {@code b} should be merged into the
	 * same result element.
	 */
	boolean areEquivalent(ElementView a, ElementView b);
}
