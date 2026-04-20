package ca.mcscert.jpipe.operators.equivalences;

import ca.mcscert.jpipe.model.elements.ElementView;
import ca.mcscert.jpipe.operators.EquivalenceRelation;

/**
 * Two elements are equivalent iff their ids are identical after stripping any
 * namespace prefix (the last colon-separated segment). For example,
 * {@code "a:s"} and {@code "b:s"} both have short id {@code "s"} and are
 * therefore equivalent.
 */
public final class SameShortId implements EquivalenceRelation {

	@Override
	public boolean areEquivalent(ElementView a, ElementView b) {
		return shortId(a.id()).equals(shortId(b.id()));
	}

	private static String shortId(String id) {
		int colon = id.lastIndexOf(':');
		return colon >= 0 ? id.substring(colon + 1) : id;
	}
}
