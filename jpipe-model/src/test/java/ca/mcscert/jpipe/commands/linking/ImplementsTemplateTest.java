package ca.mcscert.jpipe.commands.linking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ImplementsTemplateTest {

	// -------------------------------------------------------------------------
	// Normal execution
	// -------------------------------------------------------------------------

	@Test
	void setsTemplateAsParentOfJustification() throws Exception {
		Unit unit = new Unit("src");
		new CreateTemplate("t1").execute(unit);
		new CreateJustification("j1").execute(unit);

		new ImplementsTemplate("j1", "t1").execute(unit);

		assertThat(unit.get("j1").getParent()).isPresent().get().extracting(Template::getName).isEqualTo("t1");
	}

	@Test
	void templateCanHaveMultipleImplementors() throws Exception {
		Unit unit = new Unit("src");
		new CreateTemplate("t1").execute(unit);
		new CreateJustification("j1").execute(unit);
		new CreateJustification("j2").execute(unit);

		new ImplementsTemplate("j1", "t1").execute(unit);
		new ImplementsTemplate("j2", "t1").execute(unit);

		assertThat(unit.get("j1").getParent()).isPresent();
		assertThat(unit.get("j2").getParent()).isPresent();
	}

	// -------------------------------------------------------------------------
	// Error cases
	// -------------------------------------------------------------------------

	@Test
	void throwsWhenNamedModelIsNotATemplate() throws Exception {
		Unit unit = new Unit("src");
		new CreateJustification("j1").execute(unit);
		new CreateJustification("j2").execute(unit);

		assertThatThrownBy(() -> new ImplementsTemplate("j1", "j2").execute(unit))
				.isInstanceOf(NoSuchElementException.class);
	}

	// -------------------------------------------------------------------------
	// Condition / deferred execution
	// -------------------------------------------------------------------------

	@Nested
	class DeferredExecution {

		@Test
		void conditionFalseWhenBothModelsAbsent() {
			Unit unit = new Unit("src");
			assertThat(new ImplementsTemplate("j1", "t1").condition().test(unit)).isFalse();
		}

		@Test
		void conditionFalseWhenOnlyJustificationExists() throws Exception {
			Unit unit = new Unit("src");
			new CreateJustification("j1").execute(unit);
			assertThat(new ImplementsTemplate("j1", "t1").condition().test(unit)).isFalse();
		}

		@Test
		void conditionFalseWhenOnlyTemplateExists() throws Exception {
			Unit unit = new Unit("src");
			new CreateTemplate("t1").execute(unit);
			assertThat(new ImplementsTemplate("j1", "t1").condition().test(unit)).isFalse();
		}

		@Test
		void conditionTrueWhenBothModelsExist() throws Exception {
			Unit unit = new Unit("src");
			new CreateJustification("j1").execute(unit);
			new CreateTemplate("t1").execute(unit);
			assertThat(new ImplementsTemplate("j1", "t1").condition().test(unit)).isTrue();
		}

		@Test
		void engineDefersUntilBothModelsCreated() {
			ExecutionEngine engine = new ExecutionEngine();
			Unit unit = engine.spawn("src", List.of(new ImplementsTemplate("j1", "t1"), new CreateTemplate("t1"),
					new CreateJustification("j1")));

			assertThat(unit.get("j1").getParent()).isPresent().get().isInstanceOf(Template.class)
					.extracting(m -> ((Template) m).getName()).isEqualTo("t1");
		}
	}

	// -------------------------------------------------------------------------
	// Template inlining — qualified ids and support-edge re-wiring
	// -------------------------------------------------------------------------

	@Nested
	class TemplateInlining {

		/**
		 * Builds a unit with template t (tc←ts←te) and justification j implements t.
		 */
		private Unit buildUnit() {
			ExecutionEngine engine = new ExecutionEngine();
			return engine.spawn("src",
					List.of(new CreateTemplate("t"), new CreateConclusion("t", "tc", "Template conclusion"),
							new CreateStrategy("t", "ts", "Template strategy"),
							new CreateEvidence("t", "te", "Template evidence"), new AddSupport("t", "tc", "ts"),
							new AddSupport("t", "ts", "te"), new CreateJustification("j"),
							new CreateConclusion("j", "c", "My conclusion"), new ImplementsTemplate("j", "t")));
		}

		@Test
		void templateConclusionIsExpandedAsSubConclusion() {
			Unit unit = buildUnit();
			assertThat(unit.get("j").findById("t:tc")).isPresent().get().isInstanceOf(SubConclusion.class);
		}

		@Test
		void templateStrategyIsExpandedWithQualifiedId() {
			Unit unit = buildUnit();
			assertThat(unit.get("j").findById("t:ts")).isPresent().get().isInstanceOf(Strategy.class);
		}

		@Test
		void templateEvidenceIsExpandedWithQualifiedId() {
			Unit unit = buildUnit();
			assertThat(unit.get("j").findById("t:te")).isPresent().get().isInstanceOf(Evidence.class);
		}

		@Test
		void plainIdResolvesToInheritedElement() {
			Unit unit = buildUnit();
			assertThat(unit.get("j").findById("ts")).isPresent().get().isInstanceOf(Strategy.class)
					.extracting(e -> ((Strategy) e).id()).isEqualTo("t:ts");
		}

		@Test
		void supportEdgeBetweenCopiedElementsIsRewired() {
			Unit unit = buildUnit();
			SubConclusion tc = (SubConclusion) unit.get("j").findById("t:tc").orElseThrow();
			assertThat(tc.getSupport()).isPresent().get().extracting(Strategy::id).isEqualTo("t:ts");
		}

		@Test
		void overriddenAbstractSupportIsNotCopied() {
			ExecutionEngine engine = new ExecutionEngine();
			Unit unit = engine.spawn("src",
					List.of(new CreateTemplate("t"), new CreateConclusion("t", "tc", "Template conclusion"),
							new CreateStrategy("t", "ts", "Template strategy"),
							new CreateAbstractSupport("t", "as", "Abstract support"), new AddSupport("t", "ts", "as"),
							new AddSupport("t", "tc", "ts"), new CreateJustification("j"),
							new CreateConclusion("j", "c", "My conclusion"),
							new CreateEvidence("j", "as", "Concrete evidence"), new ImplementsTemplate("j", "t")));

			Justification j = (Justification) unit.get("j");
			assertThat(j.findById("t:as")).isEmpty();
			assertThat(j.findById("as")).isPresent().get().isInstanceOf(Evidence.class);
		}

		@Test
		void multipleImplementorsExpandIndependently() {
			ExecutionEngine engine = new ExecutionEngine();
			Unit unit = engine.spawn("src",
					List.of(new CreateTemplate("t"), new CreateConclusion("t", "tc", "Template conclusion"),
							new CreateStrategy("t", "ts", "Template strategy"),
							new CreateEvidence("t", "te", "Template evidence"), new AddSupport("t", "tc", "ts"),
							new AddSupport("t", "ts", "te"), new CreateJustification("j1"),
							new CreateConclusion("j1", "c", "Conclusion 1"), new CreateJustification("j2"),
							new CreateConclusion("j2", "c", "Conclusion 2"), new ImplementsTemplate("j1", "t"),
							new ImplementsTemplate("j2", "t")));

			Strategy s1 = (Strategy) unit.get("j1").findById("t:ts").orElseThrow();
			Strategy s2 = (Strategy) unit.get("j2").findById("t:ts").orElseThrow();
			assertThat(s1).isNotSameAs(s2);
		}
	}

	// -------------------------------------------------------------------------
	// Template-to-template inlining
	// -------------------------------------------------------------------------

	@Nested
	class TemplateToTemplateInlining {

		@Test
		void childTemplateInheritsParentElementsWithQualifiedIds() {
			ExecutionEngine engine = new ExecutionEngine();
			Unit unit = engine.spawn("src",
					List.of(new CreateTemplate("parent"), new CreateConclusion("parent", "pc", "Parent conclusion"),
							new CreateStrategy("parent", "ps", "Parent strategy"),
							new CreateAbstractSupport("parent", "as", "Abstract support"),
							new AddSupport("parent", "ps", "as"), new AddSupport("parent", "pc", "ps"),
							new CreateTemplate("child"), new CreateConclusion("child", "cc", "Child conclusion"),
							new ImplementsTemplate("child", "parent")));

			assertThat(unit.get("child").findById("parent:pc")).isPresent().get().isInstanceOf(SubConclusion.class);
			assertThat(unit.get("child").findById("parent:ps")).isPresent().get().isInstanceOf(Strategy.class);
		}

		@Test
		void childTemplateInheritsAbstractSupport() {
			ExecutionEngine engine = new ExecutionEngine();
			Unit unit = engine.spawn("src",
					List.of(new CreateTemplate("parent"), new CreateConclusion("parent", "pc", "Parent conclusion"),
							new CreateAbstractSupport("parent", "as", "Abstract support"), new CreateTemplate("child"),
							new CreateConclusion("child", "cc", "Child conclusion"),
							new ImplementsTemplate("child", "parent")));

			assertThat(unit.get("child").findById("parent:as")).isPresent().get().isInstanceOf(AbstractSupport.class);
		}

		@Test
		void childTemplateSetsParentReference() {
			ExecutionEngine engine = new ExecutionEngine();
			Unit unit = engine.spawn("src",
					List.of(new CreateTemplate("parent"), new CreateConclusion("parent", "pc", "Parent conclusion"),
							new CreateAbstractSupport("parent", "as", "Abstract support"), new CreateTemplate("child"),
							new CreateConclusion("child", "cc", "Child conclusion"),
							new ImplementsTemplate("child", "parent")));

			assertThat(unit.get("child").getParent()).isPresent().get().extracting(Template::getName)
					.isEqualTo("parent");
		}
	}

	// -------------------------------------------------------------------------
	// toString
	// -------------------------------------------------------------------------

	@Test
	void toStringIsPrologFact() {
		assertThat(new ImplementsTemplate("j1", "t1").toString()).isEqualTo("implements('j1', 't1').");
	}
}
