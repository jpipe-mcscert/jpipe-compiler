package ca.mcscert.jpipe.model.elements;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class StrategyTest {

	@Test
	void addSupport_appendsDistinctSupporters() {
		Strategy s = new Strategy("s", "S");
		Evidence e1 = new Evidence("e1", "One");
		Evidence e2 = new Evidence("e2", "Two");

		s.addSupport(e1);
		s.addSupport(e2);

		assertThat(s.getSupports()).containsExactly(e1, e2);
	}

	@Test
	void addSupport_isIdempotentById() {
		// A strategy can never legitimately be supported twice by the same id.
		Strategy s = new Strategy("s", "S");
		AbstractSupport abs = new AbstractSupport("x", "Abstract");
		Evidence sameId = new Evidence("x", "Concrete with same id");

		s.addSupport(abs);
		s.addSupport(sameId);

		assertThat(s.getSupports()).containsExactly(abs);
	}

	@Test
	void replaceSupport_matchesByIdNotObjectIdentity() {
		// The abstract placeholder and its concrete override share the same id
		// but are different objects/types; replaceSupport must still swap them.
		Strategy s = new Strategy("s", "S");
		AbstractSupport abs = new AbstractSupport("x", "Abstract");
		Evidence concrete = new Evidence("x", "Concrete");
		s.addSupport(abs);

		s.replaceSupport(concrete, concrete);

		assertThat(s.getSupports()).containsExactly(concrete);
	}
}
