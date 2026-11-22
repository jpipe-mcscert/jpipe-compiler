package ca.mcscert.jpipe.operators;

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
}
