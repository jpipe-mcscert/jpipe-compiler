package ca.mcscert.jpipe.model.elements;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.visitor.JustificationVisitor;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JustificationElementTest {

	/**
	 * A visitor that records which concrete type was visited, for dispatch
	 * tests.
	 */
	private static class RecordingVisitor
			implements
				JustificationVisitor<Class<?>> {
		@Override
		public Class<?> visit(Unit u) {
			return Unit.class;
		}
		@Override
		public Class<?> visit(Justification j) {
			return Justification.class;
		}
		@Override
		public Class<?> visit(Template t) {
			return Template.class;
		}
		@Override
		public Class<?> visit(Conclusion c) {
			return Conclusion.class;
		}
		@Override
		public Class<?> visit(SubConclusion sc) {
			return SubConclusion.class;
		}
		@Override
		public Class<?> visit(Strategy s) {
			return Strategy.class;
		}
		@Override
		public Class<?> visit(Evidence e) {
			return Evidence.class;
		}
		@Override
		public Class<?> visit(AbstractSupport as) {
			return AbstractSupport.class;
		}
	}

	private final RecordingVisitor visitor = new RecordingVisitor();

	@Nested
	class ConclusionTest {
		private final Conclusion conclusion = new Conclusion("c1",
				"my conclusion");

		@Test
		void storesId() {
			assertThat(conclusion.id()).isEqualTo("c1");
		}
		@Test
		void storesLabel() {
			assertThat(conclusion.label()).isEqualTo("my conclusion");
		}
		@Test
		void acceptDispatchesToVisitor() {
			assertThat(conclusion.accept(visitor)).isEqualTo(Conclusion.class);
		}
		@Test
		void addSupportRegistersStrategy() {
			Conclusion c = new Conclusion("c1", "conclusion");
			Strategy s = new Strategy("s1", "strategy");
			c.addSupport(s);
			assertThat(c.getSupport()).isPresent().contains(s);
		}
		@Test
		void addSupportThrowsWhenAlreadySet() {
			Conclusion c = new Conclusion("c1", "conclusion");
			Strategy s = new Strategy("s1", "strategy");
			c.addSupport(s);
			assertThatThrownBy(() -> c.addSupport(s))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("Conclusion");
		}
		@Test
		void toStringContainsIdAndLabel() {
			assertThat(conclusion)
					.hasToString("Conclusion{id='c1', label='my conclusion'}");
		}
	}

	@Nested
	class SubConclusionTest {
		private final SubConclusion subConclusion = new SubConclusion("sc1",
				"my sub-conclusion");

		@Test
		void storesId() {
			assertThat(subConclusion.id()).isEqualTo("sc1");
		}
		@Test
		void storesLabel() {
			assertThat(subConclusion.label()).isEqualTo("my sub-conclusion");
		}
		@Test
		void acceptDispatchesToVisitor() {
			assertThat(subConclusion.accept(visitor))
					.isEqualTo(SubConclusion.class);
		}
		@Test
		void addSupportRegistersStrategy() {
			SubConclusion sc = new SubConclusion("sc1", "sub-conclusion");
			Strategy s = new Strategy("s1", "strategy");
			sc.addSupport(s);
			assertThat(sc.getSupport()).isPresent().contains(s);
		}
		@Test
		void addSupportThrowsWhenAlreadySet() {
			SubConclusion sc = new SubConclusion("sc1", "sub-conclusion");
			Strategy s = new Strategy("s1", "strategy");
			sc.addSupport(s);
			assertThatThrownBy(() -> sc.addSupport(s))
					.isInstanceOf(IllegalStateException.class)
					.hasMessageContaining("SubConclusion");
		}
		@Test
		void toStringContainsIdAndLabel() {
			assertThat(subConclusion).hasToString(
					"SubConclusion{id='sc1', label='my sub-conclusion'}");
		}
	}

	@Nested
	class StrategyTest {
		private final Strategy strategy = new Strategy("s1", "my strategy");

		@Test
		void storesId() {
			assertThat(strategy.id()).isEqualTo("s1");
		}
		@Test
		void storesLabel() {
			assertThat(strategy.label()).isEqualTo("my strategy");
		}
		@Test
		void acceptDispatchesToVisitor() {
			assertThat(strategy.accept(visitor)).isEqualTo(Strategy.class);
		}
	}

	@Nested
	class EvidenceTest {
		private final Evidence evidence = new Evidence("e1", "my evidence");

		@Test
		void storesId() {
			assertThat(evidence.id()).isEqualTo("e1");
		}
		@Test
		void storesLabel() {
			assertThat(evidence.label()).isEqualTo("my evidence");
		}
		@Test
		void acceptDispatchesToVisitor() {
			assertThat(evidence.accept(visitor)).isEqualTo(Evidence.class);
		}
	}

	@Nested
	class AbstractSupportTest {
		private final AbstractSupport abstractSupport = new AbstractSupport(
				"as1", "my abstract support");

		@Test
		void storesId() {
			assertThat(abstractSupport.id()).isEqualTo("as1");
		}
		@Test
		void storesLabel() {
			assertThat(abstractSupport.label())
					.isEqualTo("my abstract support");
		}
		@Test
		void acceptDispatchesToVisitor() {
			assertThat(abstractSupport.accept(visitor))
					.isEqualTo(AbstractSupport.class);
		}
	}
}
