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
	// Element categorisation: ownElements / inheritedElements /
	// concreteOverrides
	// + hasOwnConclusion
	// -------------------------------------------------------------------------

	@Nested
	@DisplayName("Element categorisation (own / inherited / overrides)")
	class ElementCategorisation {

		// ------------------------------------------------------------------
		// Setup helpers — a template with conclusion c, strategy s, abstract
		// support abs; and a justification that inlines it.
		// ------------------------------------------------------------------

		private Template template() {
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
			return t;
		}

		@Test
		@DisplayName("no parent: ownElements() == getElements(), others empty")
		void noParentAllElementsAreOwn() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "conclusion"));
			j.addElement(new Strategy("s", "strategy"));

			assertThat(j.ownElements()).isEqualTo(j.getElements());
			assertThat(j.inheritedElements()).isEmpty();
			assertThat(j.concreteOverrides()).isEmpty();
		}

		@Test
		@DisplayName("no parent: hasOwnConclusion() is true when conclusion is set")
		void noParentHasOwnConclusionIsTrue() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "conclusion"));
			assertThat(j.hasOwnConclusion()).isTrue();
		}

		@Test
		@DisplayName("no parent: hasOwnConclusion() is false when conclusion is absent")
		void noParentHasOwnConclusionIsFalseWhenNoConclusionSet() {
			Justification j = new Justification("j");
			assertThat(j.hasOwnConclusion()).isFalse();
		}

		@Test
		@DisplayName("after inline into Template: all copied elements are inherited")
		void inlinedElementsAreInheritedInTemplate() {
			Template parent = template();
			Template child = new Template("child");
			child.inline(parent, "t");

			assertThat(child.ownElements()).isEmpty();
			assertThat(child.inheritedElements()).extracting(e -> e.id())
					.containsExactlyInAnyOrder("t:s", "t:abs");
			assertThat(child.concreteOverrides()).isEmpty();
		}

		@Test
		@DisplayName("after inline into Justification: CommonElement copies are inherited")
		void inlinedCommonElementsAreInheritedInJustification() {
			// Template with no AbstractSupport — safe to inline into
			// Justification
			Template t = new Template("t");
			t.setConclusion(new Conclusion("c", "conclusion"));
			Strategy ts = new Strategy("s", "strategy");
			Evidence te = new Evidence("e", "evidence");
			t.addElement(ts);
			t.addElement(te);
			ts.addSupport(te);
			t.conclusion().ifPresent(c -> c.addSupport(ts));

			Justification j = new Justification("j");
			j.inline(t, "t");

			assertThat(j.ownElements()).isEmpty();
			assertThat(j.inheritedElements()).extracting(e -> e.id())
					.containsExactlyInAnyOrder("t:s", "t:e");
			assertThat(j.concreteOverrides()).isEmpty();
		}

		@Test
		@DisplayName("after inline: conclusion is inherited, hasOwnConclusion() is false")
		void inlinedConclusionIsNotOwn() {
			Template t = template();
			Justification j = new Justification("j");
			j.inline(t, "t");

			assertThat(j.hasOwnConclusion()).isFalse();
		}

		@Test
		@DisplayName("own element added after inline into Template appears in ownElements() only")
		void localElementAfterInlineIsOwn() {
			Template parent = template();
			Template child = new Template("child");
			child.inline(parent, "t");
			child.addElement(new Evidence("e_local", "local evidence"));

			assertThat(child.ownElements()).extracting(e -> e.id())
					.containsExactly("e_local");
			assertThat(child.inheritedElements()).extracting(e -> e.id())
					.containsExactlyInAnyOrder("t:s", "t:abs");
		}

		@Test
		@DisplayName("overriding an abstract support moves it to concreteOverrides()")
		void overriddenAbstractSupportIsInConcreteOverrides() {
			Template t = template();
			Justification j = new Justification("j");
			j.inline(t, "t");
			// Manually simulate the override: remove abstract, add concrete
			// with
			// same qualified id
			j.removeElement("t:abs");
			j.addElement(new Evidence("t:abs", "concrete evidence"));

			assertThat(j.concreteOverrides()).extracting(e -> e.id())
					.containsExactly("t:abs");
			assertThat(j.inheritedElements()).extracting(e -> e.id())
					.containsExactly("t:s");
			assertThat(j.ownElements()).isEmpty();
		}

		@Test
		@DisplayName("own + inherited + overrides partition getElements()")
		void categoriesPartitionGetElements() {
			Template t = template();
			Justification j = new Justification("j");
			j.inline(t, "t");
			j.addElement(new Evidence("e_local", "local evidence"));
			j.removeElement("t:abs");
			j.addElement(new Evidence("t:abs", "concrete evidence"));

			assertThat(j.ownElements().size() + j.inheritedElements().size()
					+ j.concreteOverrides().size())
					.isEqualTo(j.getElements().size());
		}

		@Test
		@DisplayName("multi-level: grandparent elements are inherited in leaf model")
		void multiLevelInheritanceGrandparentElementsAreInherited() {
			// root template: conclusion c, strategy s, abstract supports abs1,
			// abs2
			Template root = new Template("root");
			Conclusion rc = new Conclusion("c", "root conclusion");
			root.setConclusion(rc);
			Strategy rs = new Strategy("s", "root strategy");
			root.addElement(rs);
			AbstractSupport rabs1 = new AbstractSupport("abs1", "abstract 1");
			AbstractSupport rabs2 = new AbstractSupport("abs2", "abstract 2");
			root.addElement(rabs1);
			root.addElement(rabs2);
			rs.addSupport(rabs1);
			rs.addSupport(rabs2);
			rc.addSupport(rs);

			// intermediate template implements root, overrides abs1
			Template intermediate = new Template("inter");
			intermediate.inline(root, "root");
			intermediate.removeElement("root:abs1");
			intermediate.addElement(new SubConclusion("root:abs1", "refined"));

			// leaf justification implements intermediate, overrides root:abs2
			Justification leaf = new Justification("leaf");
			leaf.inline(intermediate, "inter");
			leaf.removeElement("root:abs2");
			leaf.addElement(new Evidence("root:abs2", "evidence for abs2"));

			// root:abs2 was AbstractSupport in inter → now Evidence → override
			assertThat(leaf.concreteOverrides()).extracting(e -> e.id())
					.containsExactly("root:abs2");
			// root:abs1 was SubConclusion in inter → still SubConclusion →
			// inherited
			// root:s (strategy) — id preserved by the no-re-qualify rule
			assertThat(leaf.inheritedElements()).extracting(e -> e.id())
					.containsExactlyInAnyOrder("root:abs1", "root:s");
			assertThat(leaf.ownElements()).isEmpty();
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
