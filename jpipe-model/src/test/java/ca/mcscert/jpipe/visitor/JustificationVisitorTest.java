package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JustificationVisitorTest {

	/** Collects the id (or name) of each node visited, in order. */
	private static class CollectingVisitor implements JustificationVisitor<Void> {

		final List<String> visited = new ArrayList<>();

		@Override
		public Void visit(Unit u) {
			visited.add("unit:" + u.getSource());
			return null;
		}

		@Override
		public Void visit(Justification j) {
			visited.add("justification:" + j.getName());
			return null;
		}

		@Override
		public Void visit(Template t) {
			visited.add("template:" + t.getName());
			return null;
		}

		@Override
		public Void visit(Conclusion c) {
			visited.add("conclusion:" + c.id());
			return null;
		}

		@Override
		public Void visit(SubConclusion sc) {
			visited.add("subconclusion:" + sc.id());
			return null;
		}

		@Override
		public Void visit(Strategy s) {
			visited.add("strategy:" + s.id());
			return null;
		}

		@Override
		public Void visit(Evidence e) {
			visited.add("evidence:" + e.id());
			return null;
		}

		@Override
		public Void visit(AbstractSupport as) {
			visited.add("abstractsupport:" + as.id());
			return null;
		}
	}

	private CollectingVisitor visitor;

	@BeforeEach
	void setUp() {
		visitor = new CollectingVisitor();
	}

	@Nested
	class EmptyUnit {

		@Test
		void onlyVisitsUnit() {
			Unit unit = new Unit("test.jd");
			unit.accept(visitor);
			assertThat(visitor.visited).containsExactly("unit:test.jd");
		}
	}

	@Nested
	class UnitWithJustification {

		private Unit unit;

		@BeforeEach
		void setUp() {
			unit = new Unit("test.jd");
			Justification j = new Justification("my-justification");
			j.setConclusion(new Conclusion("c1", "a conclusion"));
			j.addElement(new Strategy("s1", "a strategy"));
			j.addElement(new Evidence("e1", "an evidence"));
			unit.add(j);
		}

		@Test
		void visitsInPreOrder() {
			unit.accept(visitor);
			assertThat(visitor.visited).containsExactly("unit:test.jd", "justification:my-justification",
					"conclusion:c1", "strategy:s1", "evidence:e1");
		}
	}

	@Nested
	class UnitWithTemplate {

		private Unit unit;

		@BeforeEach
		void setUp() {
			unit = new Unit("test.jd");
			Template t = new Template("my-template");
			t.setConclusion(new Conclusion("c1", "a conclusion"));
			t.addElement(new AbstractSupport("as1", "an abstract support"));
			unit.add(t);
		}

		@Test
		void visitsInPreOrder() {
			unit.accept(visitor);
			assertThat(visitor.visited).containsExactly("unit:test.jd", "template:my-template", "conclusion:c1",
					"abstractsupport:as1");
		}
	}

	@Nested
	class UnitWithMultipleModels {

		private Unit unit;

		@BeforeEach
		void setUp() {
			unit = new Unit("test.jd");
			Justification j1 = new Justification("j1");
			j1.setConclusion(new Conclusion("c1", "conclusion one"));
			Justification j2 = new Justification("j2");
			j2.addElement(new SubConclusion("sc1", "sub-conclusion one"));
			unit.add(j1);
			unit.add(j2);
		}

		@Test
		void visitsAllModelsInInsertionOrder() {
			unit.accept(visitor);
			assertThat(visitor.visited).containsExactly("unit:test.jd", "justification:j1", "conclusion:c1",
					"justification:j2", "subconclusion:sc1");
		}
	}
}
