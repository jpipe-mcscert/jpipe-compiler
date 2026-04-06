package ca.mcscert.jpipe.operators.equivalences;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.operators.SourcedElement;
import org.junit.jupiter.api.Test;

class SameShortIdTest {

	private final SameShortId relation = new SameShortId();
	private final Justification model = new Justification("m");

	@Test
	void equivalentWhenShortIdsMatchAfterStrippingPrefix() {
		SourcedElement a = new SourcedElement(new Strategy("a:s1", "strat"),
				model);
		SourcedElement b = new SourcedElement(new Strategy("b:s1", "other"),
				model);
		assertThat(relation.areEquivalent(a, b)).isTrue();
	}

	@Test
	void notEquivalentWhenShortIdsDiffer() {
		SourcedElement a = new SourcedElement(new Strategy("a:s1", "strat"),
				model);
		SourcedElement b = new SourcedElement(new Strategy("b:s2", "strat"),
				model);
		assertThat(relation.areEquivalent(a, b)).isFalse();
	}

	@Test
	void plainIdEquivalentToQualifiedIdWithSameSuffix() {
		SourcedElement a = new SourcedElement(new Evidence("e1", "ev"), model);
		SourcedElement b = new SourcedElement(new Evidence("a:e1", "ev"),
				model);
		assertThat(relation.areEquivalent(a, b)).isTrue();
	}

	@Test
	void reflexive() {
		SourcedElement a = new SourcedElement(new Strategy("t:s1", "s"), model);
		assertThat(relation.areEquivalent(a, a)).isTrue();
	}

	@Test
	void symmetric() {
		SourcedElement a = new SourcedElement(new Strategy("a:s1", "x"), model);
		SourcedElement b = new SourcedElement(new Strategy("b:s1", "y"), model);
		assertThat(relation.areEquivalent(a, b))
				.isEqualTo(relation.areEquivalent(b, a));
	}
}
