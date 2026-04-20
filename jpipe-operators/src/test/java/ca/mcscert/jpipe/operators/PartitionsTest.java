package ca.mcscert.jpipe.operators;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.operators.equivalences.SameLabel;
import java.util.List;
import org.junit.jupiter.api.Test;

class PartitionsTest {

	private static final Justification MODEL = new Justification("j");
	private static final EquivalenceRelation BY_LABEL = new SameLabel();

	@Test
	void empty_list_produces_no_partitions() {
		assertThat(Partitions.partition(List.of(), BY_LABEL)).isEmpty();
	}

	@Test
	void single_element_produces_one_group() {
		SourcedElement se = sourced("id1", "label-a");
		List<ElementGroup> groups = Partitions.partition(List.of(se), BY_LABEL);
		assertThat(groups).hasSize(1);
		assertThat(groups.get(0).members()).containsExactly(se);
	}

	@Test
	void equivalent_elements_are_grouped_together() {
		SourcedElement se1 = sourced("id1", "same");
		SourcedElement se2 = sourced("id2", "same");
		List<ElementGroup> groups = Partitions.partition(List.of(se1, se2),
				BY_LABEL);
		assertThat(groups).hasSize(1);
		assertThat(groups.get(0).members()).containsExactly(se1, se2);
	}

	@Test
	void non_equivalent_elements_form_separate_groups() {
		SourcedElement se1 = sourced("id1", "alpha");
		SourcedElement se2 = sourced("id2", "beta");
		List<ElementGroup> groups = Partitions.partition(List.of(se1, se2),
				BY_LABEL);
		assertThat(groups).hasSize(2);
	}

	@Test
	void mixed_elements_partition_into_correct_groups() {
		SourcedElement se1 = sourced("id1", "alpha");
		SourcedElement se2 = sourced("id2", "beta");
		SourcedElement se3 = sourced("id3", "alpha");
		List<ElementGroup> groups = Partitions.partition(List.of(se1, se2, se3),
				BY_LABEL);
		assertThat(groups).hasSize(2);
		assertThat(groups.get(0).members()).containsExactly(se1, se3);
		assertThat(groups.get(1).members()).containsExactly(se2);
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static SourcedElement sourced(String id, String label) {
		return new SourcedElement(new Evidence(id, label), MODEL,
				SourceLocation.UNKNOWN);
	}
}
