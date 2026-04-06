package ca.mcscert.jpipe.operators.equivalences;

import ca.mcscert.jpipe.operators.EquivalenceRelation;
import ca.mcscert.jpipe.operators.SourcedElement;

/**
 * Two elements are equivalent iff they share the same label, regardless of
 * their ids or source models.
 */
public final class SameLabel implements EquivalenceRelation {

	@Override
	public boolean areEquivalent(SourcedElement a, SourcedElement b) {
		return a.element().label().equals(b.element().label());
	}
}
