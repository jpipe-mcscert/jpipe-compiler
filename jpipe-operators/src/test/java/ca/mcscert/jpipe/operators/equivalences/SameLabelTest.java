package ca.mcscert.jpipe.operators.equivalences;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.operators.SourcedElement;
import org.junit.jupiter.api.Test;

class SameLabelTest {

	private final SameLabel relation = new SameLabel();
	private final Justification model = new Justification("m");

	@Test
	void equivalentWhenLabelsMatch() {
		SourcedElement a = new SourcedElement(
				new Evidence("a:e1", "my evidence"), model);
		SourcedElement b = new SourcedElement(
				new Evidence("b:e1", "my evidence"), model);
		assertThat(relation.areEquivalent(a, b)).isTrue();
	}

	@Test
	void notEquivalentWhenLabelsDiffer() {
		SourcedElement a = new SourcedElement(
				new Evidence("a:e1", "evidence A"), model);
		SourcedElement b = new SourcedElement(
				new Evidence("b:e1", "evidence B"), model);
		assertThat(relation.areEquivalent(a, b)).isFalse();
	}

	@Test
	void reflexive() {
		SourcedElement a = new SourcedElement(new Strategy("s1", "a strategy"),
				model);
		assertThat(relation.areEquivalent(a, a)).isTrue();
	}

	@Test
	void symmetric() {
		SourcedElement a = new SourcedElement(new Evidence("a:e", "shared"),
				model);
		SourcedElement b = new SourcedElement(new Evidence("b:e", "shared"),
				model);
		assertThat(relation.areEquivalent(a, b))
				.isEqualTo(relation.areEquivalent(b, a));
	}

	@Test
	void differentTypesWithSameLabelAreEquivalent() {
		SourcedElement a = new SourcedElement(new Evidence("e1", "same label"),
				model);
		SourcedElement b = new SourcedElement(new Strategy("s1", "same label"),
				model);
		assertThat(relation.areEquivalent(a, b)).isTrue();
	}
}
