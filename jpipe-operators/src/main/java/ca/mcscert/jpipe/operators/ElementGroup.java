package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.List;

/**
 * One cell of the partition computed by {@link CompositionOperator}: a
 * non-empty list of {@link SourcedElement}s that are all equivalent under the
 * operator's {@link EquivalenceRelation}.
 */
public record ElementGroup(List<SourcedElement> members) {

	public ElementGroup {
		members = List.copyOf(members);
	}

	/** The unwrapped elements, in the same order as {@link #members()}. */
	public List<JustificationElement> elements() {
		return members.stream().map(SourcedElement::element).toList();
	}
}
