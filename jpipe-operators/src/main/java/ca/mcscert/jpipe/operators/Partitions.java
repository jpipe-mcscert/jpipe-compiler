package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.model.elements.ElementView;
import java.util.ArrayList;
import java.util.List;

/**
 * Package-private utility for partitioning a list of {@link ElementView}s into
 * equivalence classes using an {@link EquivalenceRelation}.
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
	 * Returns raw lists; each list holds elements of the concrete type
	 * {@code T}.
	 */
	static <T extends ElementView> List<List<T>> partitionBy(List<T> elements,
			EquivalenceRelation rel) {
		List<List<T>> partitions = new ArrayList<>();
		for (T candidate : elements) {
			boolean placed = false;
			for (List<T> partition : partitions) {
				if (rel.areEquivalent(partition.get(0), candidate)) {
					partition.add(candidate);
					placed = true;
					break;
				}
			}
			if (!placed) {
				List<T> fresh = new ArrayList<>();
				fresh.add(candidate);
				partitions.add(fresh);
			}
		}
		return partitions;
	}

	/**
	 * Convenience wrapper for Phase 1 composition: partitions
	 * {@link SourcedElement}s and wraps each group in an {@link ElementGroup}.
	 */
	static List<ElementGroup> partition(List<SourcedElement> elements,
			EquivalenceRelation rel) {
		return partitionBy(elements, rel).stream().map(ElementGroup::new)
				.toList();
	}
}
