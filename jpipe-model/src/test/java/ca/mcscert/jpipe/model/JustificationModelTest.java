package ca.mcscert.jpipe.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Justification Model")
class JustificationModelTest {

	// -------------------------------------------------------------------------
	// JustificationModel: parent, findById, elementsOfType
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("Base functionality (parent, findById, elementsOfType)")
	class JustificationModelBase {

		private Justification model;

		@BeforeEach
		void setUp() {
			model = new Justification("j1");
			model.setConclusion(new Conclusion("c1", "first conclusion"));
			model.addElement(new Strategy("s1", "a strategy"));
			model.addElement(new SubConclusion("sc1", "a sub-conclusion"));
		}

		@Test
		@DisplayName("parent is absent by default")
		void parentIsAbsentByDefault() {
			assertThat(model.getParent()).isEmpty();
		}

		@Test
		@DisplayName("setting and getting parent works (round-trip)")
		void setParentRoundTrips() {
			Template t = new Template("tmpl");
			model.setParent(t);
			assertThat(model.getParent()).contains(t);
		}

		@Test
		@DisplayName("findById returns the conclusion if the ID matches")
		void findByIdReturnsConclusionFromField() {
			assertThat(model.findById("c1")).isPresent().get()
					.isInstanceOf(Conclusion.class);
		}

		@Test
		@DisplayName("findById returns an element from the internal list if the ID matches")
		void findByIdReturnsElementFromList() {
			assertThat(model.findById("s1")).isPresent().get()
					.isInstanceOf(Strategy.class);
		}

		@Test
		@DisplayName("findById returns empty when the ID is not found")
		void findByIdReturnsEmptyWhenAbsent() {
			assertThat(model.findById("unknown")).isEmpty();
		}

		@Test
		@DisplayName("elementsOfType filters elements correctly by class")
		void elementsOfTypeFiltersCorrectly() {
			assertThat(model.elementsOfType(SubConclusion.class)).hasSize(1)
					.extracting(SubConclusion::id).containsExactly("sc1");
		}

		@Test
		@DisplayName("elementsOfType returns empty list when no elements match the type")
		void elementsOfTypeReturnsEmptyWhenNoMatch() {
			assertThat(model.elementsOfType(Evidence.class)).isEmpty();
		}
	}

	// -------------------------------------------------------------------------
	// Justification typed-filter methods
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("Justification-specific typed filters")
	class JustificationFilters {

		private Justification j;

		@BeforeEach
		void setUp() {
			j = new Justification("j1");
			j.setConclusion(new Conclusion("c1", "a conclusion"));
			j.addElement(new SubConclusion("sc1", "a sub-conclusion"));
			j.addElement(new Strategy("s1", "a strategy"));
			j.addElement(new Evidence("e1", "an evidence"));
		}

		@Test
		@DisplayName("conclusion() returns the primary conclusion")
		void conclusionReturnsTheConclusion() {
			assertThat(j.conclusion()).isPresent().get()
					.extracting(Conclusion::id).isEqualTo("c1");
		}

		@Test
		@DisplayName("subConclusions() returns only sub-conclusion elements")
		void subConclusionsReturnsOnlySubConclusions() {
			assertThat(j.subConclusions()).extracting(SubConclusion::id)
					.containsExactly("sc1");
		}

		@Test
		@DisplayName("strategies() returns only strategy elements")
		void strategiesReturnsOnlyStrategies() {
			assertThat(j.strategies()).extracting(Strategy::id)
					.containsExactly("s1");
		}

		@Test
		@DisplayName("evidence() returns only evidence elements")
		void evidenceReturnsOnlyEvidence() {
			assertThat(j.evidence()).extracting(Evidence::id)
					.containsExactly("e1");
		}
	}

	// -------------------------------------------------------------------------
	// Template typed-filter methods
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("Template-specific typed filters")
	class TemplateFilters {

		private Template t;

		@BeforeEach
		void setUp() {
			t = new Template("t1");
			t.setConclusion(new Conclusion("c1", "a conclusion"));
			t.addElement(new SubConclusion("sc1", "a sub-conclusion"));
			t.addElement(new Strategy("s1", "a strategy"));
			t.addElement(new Evidence("e1", "an evidence"));
			t.addElement(new AbstractSupport("as1", "an abstract support"));
		}

		@Test
		@DisplayName("conclusion() returns the primary conclusion")
		void conclusionReturnsTheConclusion() {
			assertThat(t.conclusion()).isPresent().get()
					.extracting(Conclusion::id).isEqualTo("c1");
		}

		@Test
		@DisplayName("subConclusions() returns only sub-conclusion elements")
		void subConclusionsReturnsOnlySubConclusions() {
			assertThat(t.subConclusions()).extracting(SubConclusion::id)
					.containsExactly("sc1");
		}

		@Test
		@DisplayName("strategies() returns only strategy elements")
		void strategiesReturnsOnlyStrategies() {
			assertThat(t.strategies()).extracting(Strategy::id)
					.containsExactly("s1");
		}

		@Test
		@DisplayName("evidence() returns only evidence elements")
		void evidenceReturnsOnlyEvidence() {
			assertThat(t.evidence()).extracting(Evidence::id)
					.containsExactly("e1");
		}

		@Test
		@DisplayName("abstractSupports() returns only abstract support elements")
		void abstractSupportsReturnsOnlyAbstractSupports() {
			assertThat(t.abstractSupports()).extracting(AbstractSupport::id)
					.containsExactly("as1");
		}
	}

	// -------------------------------------------------------------------------
	// JustificationModel#validate — completeness and consistency
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("Model validation (completeness and consistency)")
	class JustificationValidate {

		private Conclusion conclusion;
		private Strategy strategy;
		private Evidence evidence;

		@BeforeEach
		void setUp() {
			conclusion = new Conclusion("c1", "a conclusion");
			strategy = new Strategy("s1", "a strategy");
			evidence = new Evidence("e1", "an evidence");
		}

		private Justification complete() {
			conclusion.addSupport(strategy);
			strategy.addSupport(evidence);
			Justification j = new Justification("j1");
			j.setConclusion(conclusion);
			j.addElement(strategy);
			j.addElement(evidence);
			return j;
		}

		@Test
		@DisplayName("validate() returns no violations for a complete model")
		void validateReturnsNoViolationsForCompleteModel() {
			assertThat(complete().validate()).isEmpty();
		}

		@Test
		@DisplayName("validate() reports error when conclusion is missing")
		void validateReportsMissingConclusion() {
			Justification j = new Justification("j1");
			assertThat(j.validate()).anySatisfy(
					v -> assertThat(v.rule()).isEqualTo("conclusion-present"));
		}

		@Test
		@DisplayName("validate() reports error when conclusion has no supports")
		void validateReportsUnsupportedConclusion() {
			Justification j = new Justification("j1");
			j.setConclusion(conclusion);
			assertThat(j.validate()).anySatisfy(v -> assertThat(v.rule())
					.isEqualTo("conclusion-supported"));
		}

		@Test
		@DisplayName("validate() reports error when a strategy has no supports")
		void validateReportsUnsupportedStrategy() {
			conclusion.addSupport(strategy);
			Justification j = new Justification("j1");
			j.setConclusion(conclusion);
			j.addElement(strategy);
			assertThat(j.validate()).anySatisfy(
					v -> assertThat(v.rule()).isEqualTo("strategy-supported"));
		}

		@Test
		@DisplayName("validate() reports error when a sub-conclusion has no supports")
		void validateReportsUnsupportedSubConclusion() {
			SubConclusion sc = new SubConclusion("sc1", "a sub-conclusion");
			strategy.addSupport(sc);
			conclusion.addSupport(strategy);
			Justification j = new Justification("j1");
			j.setConclusion(conclusion);
			j.addElement(strategy);
			j.addElement(sc);
			assertThat(j.validate()).anySatisfy(v -> assertThat(v.rule())
					.isEqualTo("sub-conclusion-supported"));
		}

		@Test
		@DisplayName("validate() accumulates multiple violations")
		void validateAccumulatesAllViolations() {
			Justification j = new Justification("j1");
			j.setConclusion(conclusion);
			j.addElement(strategy);
			// conclusion unsupported + strategy unsupported = 2 violations
			assertThat(j.validate()).hasSizeGreaterThanOrEqualTo(2);
		}

		@Test
		@DisplayName("validate() reports cycle in 'implements' hierarchy")
		void validateReportsImplementsCycle() {
			Template t1 = new Template("t1");
			Template t2 = new Template("t2");
			t1.inline(t2, "t2"); // t1.parent = t2
			t2.inline(t1, "t1"); // t2.parent = t1

			assertThat(t1.validate()).anySatisfy(
					v -> assertThat(v.rule()).isEqualTo("acyclic-implements"));
		}

		@Test
		@DisplayName("validate() reports error when an abstract support is not overridden in instance")
		void validateReportsAbstractSupportNotOverridden() {
			Template t = new Template("t");
			Conclusion tc = new Conclusion("c", "template conclusion");
			t.setConclusion(tc);
			Strategy ts = new Strategy("s", "template strategy");
			t.addElement(ts);
			AbstractSupport tabs = new AbstractSupport("abs",
					"abstract support");
			t.addElement(tabs);
			ts.addSupport(tabs);
			tc.addSupport(ts);

			Justification j = new Justification("j");
			j.inline(t, "t");

			assertThat(j.validate()).anySatisfy(
					v -> assertThat(v.rule()).isEqualTo("no-abstract-support"));
		}
	}

	// -------------------------------------------------------------------------
	// Unit query methods
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("Unit-level queries (templates, retrieval)")
	class UnitQueries {

		private Unit unit;

		@BeforeEach
		void setUp() {
			unit = new Unit("src");
			unit.add(new Justification("j1"));
			unit.add(new Template("t1"));
		}

		@Test
		@DisplayName("templates() returns only template units")
		void templatesReturnsOnlyTemplates() {
			assertThat(unit.templates()).hasSize(1)
					.extracting(Template::getName).containsExactly("t1");
		}

		@Test
		@DisplayName("get() throws exception for unknown element name")
		void getThrowsForUnknownName() {
			assertThatThrownBy(() -> unit.get("nonexistent"))
					.isInstanceOf(NoSuchElementException.class)
					.hasMessageContaining("nonexistent");
		}
	}
}
