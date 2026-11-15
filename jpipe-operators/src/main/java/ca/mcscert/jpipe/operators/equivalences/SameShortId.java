package ca.mcscert.jpipe.operators.equivalences;

import ca.mcscert.jpipe.model.elements.ElementView;
import ca.mcscert.jpipe.operators.EquivalenceRelation;
import ca.mcscert.jpipe.util.QualifiedIds;

/**
 * Two elements are equivalent iff their ids are identical after stripping any
 * namespace prefix (the last colon-separated segment). For example,
 * {@code "a:s"} and {@code "b:s"} both have short id {@code "s"} and are
 * therefore equivalent.
 */
public final class SameShortId implements EquivalenceRelation {

	@Override
	public boolean areEquivalent(ElementView a, ElementView b) {
		return QualifiedIds.shortId(a.id())
				.equals(QualifiedIds.shortId(b.id()));
	}
}
