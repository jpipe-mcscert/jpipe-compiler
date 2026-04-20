package ca.mcscert.jpipe.operators.equivalences;

import ca.mcscert.jpipe.model.elements.ElementView;
import ca.mcscert.jpipe.operators.EquivalenceRelation;

/**
 * Two elements are equivalent iff they share the same label, regardless of
 * their ids or source models.
 */
public final class SameLabel implements EquivalenceRelation {

	@Override
	public boolean areEquivalent(ElementView a, ElementView b) {
		return a.label().equals(b.label());
	}
}
