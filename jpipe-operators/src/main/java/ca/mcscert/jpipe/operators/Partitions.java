package ca.mcscert.jpipe.operators;

import java.util.ArrayList;
import java.util.List;

/**
 * Package-private utility for partitioning a list of {@link SourcedElement}s
 * into equivalence classes using an {@link EquivalenceRelation}.
 *
 * <p>
 * Extracted from {@link CompositionOperator} so that {@link Unifier} can reuse
 * the same O(n²) representative-based scan without duplication.
 */
final class Partitions {

	private Partitions() {
	}

	/**
	 * Partitions {@code elements} into groups where every member of a group is
	 * equivalent (according to {@code rel}) to the first member of that group.
	 */
	static List<ElementGroup> partition(List<SourcedElement> elements,
			EquivalenceRelation rel) {
		List<List<SourcedElement>> partitions = new ArrayList<>();
		for (SourcedElement candidate : elements) {
			boolean placed = false;
			for (List<SourcedElement> partition : partitions) {
				if (rel.areEquivalent(partition.get(0), candidate)) {
					partition.add(candidate);
					placed = true;
					break;
				}
			}
			if (!placed) {
				List<SourcedElement> fresh = new ArrayList<>();
				fresh.add(candidate);
				partitions.add(fresh);
			}
		}
		return partitions.stream().map(ElementGroup::new).toList();
	}
}
