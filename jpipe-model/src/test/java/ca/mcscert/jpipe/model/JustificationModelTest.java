package ca.mcscert.jpipe.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.exceptions.IncompleteJustificationException;
import ca.mcscert.jpipe.model.exceptions.LockedModelException;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class JustificationModelTest {

	// -------------------------------------------------------------------------
	// JustificationModel: parent, findById, elementsOfType
	// -------------------------------------------------------------------------

	@Nested
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
		void parentIsAbsentByDefault() {
			assertThat(model.getParent()).isEmpty();
		}

		@Test
		void setParentRoundTrips() {
			Template t = new Template("tmpl");
			model.setParent(t);
			assertThat(model.getParent()).contains(t);
		}

		@Test
		void findByIdReturnsConclusionFromField() {
			assertThat(model.findById("c1")).isPresent().get()
					.isInstanceOf(Conclusion.class);
		}

		@Test
		void findByIdReturnsElementFromList() {
			assertThat(model.findById("s1")).isPresent().get()
					.isInstanceOf(Strategy.class);
		}

		@Test
		void findByIdReturnsEmptyWhenAbsent() {
			assertThat(model.findById("unknown")).isEmpty();
		}

		@Test
		void elementsOfTypeFiltersCorrectly() {
			assertThat(model.elementsOfType(SubConclusion.class)).hasSize(1)
					.extracting(SubConclusion::id).containsExactly("sc1");
		}

		@Test
		void elementsOfTypeReturnsEmptyWhenNoMatch() {
			assertThat(model.elementsOfType(Evidence.class)).isEmpty();
		}
	}

	// -------------------------------------------------------------------------
	// Justification typed-filter methods
	// -------------------------------------------------------------------------

	@Nested
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
		void conclusionReturnsTheConclusion() {
			assertThat(j.conclusion()).isPresent().get()
					.extracting(Conclusion::id).isEqualTo("c1");
		}

		@Test
		void subConclusionsReturnsOnlySubConclusions() {
			assertThat(j.subConclusions()).extracting(SubConclusion::id)
					.containsExactly("sc1");
		}

		@Test
		void strategiesReturnsOnlyStrategies() {
			assertThat(j.strategies()).extracting(Strategy::id)
					.containsExactly("s1");
		}

		@Test
		void evidenceReturnsOnlyEvidence() {
			assertThat(j.evidence()).extracting(Evidence::id)
					.containsExactly("e1");
		}
	}

	// -------------------------------------------------------------------------
	// Template typed-filter methods
	// -------------------------------------------------------------------------

	@Nested
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
		void conclusionReturnsTheConclusion() {
			assertThat(t.conclusion()).isPresent().get()
					.extracting(Conclusion::id).isEqualTo("c1");
		}

		@Test
		void subConclusionsReturnsOnlySubConclusions() {
			assertThat(t.subConclusions()).extracting(SubConclusion::id)
					.containsExactly("sc1");
		}

		@Test
		void strategiesReturnsOnlyStrategies() {
			assertThat(t.strategies()).extracting(Strategy::id)
					.containsExactly("s1");
		}

		@Test
		void evidenceReturnsOnlyEvidence() {
			assertThat(t.evidence()).extracting(Evidence::id)
					.containsExactly("e1");
		}

		@Test
		void abstractSupportsReturnsOnlyAbstractSupports() {
			assertThat(t.abstractSupports()).extracting(AbstractSupport::id)
					.containsExactly("as1");
		}
	}

	// -------------------------------------------------------------------------
	// Justification#lock — completeness validation
	// -------------------------------------------------------------------------

	@Nested
	class JustificationLock {

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
		void lockSucceedsWhenAllElementsAreSupported() {
			assertThat(complete()).satisfies(
					j -> assertThatCode(j::lock).doesNotThrowAnyException());
		}

		@Test
		void lockThrowsWhenConclusionHasNoSupport() {
			Justification j = new Justification("j1");
			j.setConclusion(conclusion);
			assertThatThrownBy(j::lock)
					.isInstanceOf(IncompleteJustificationException.class)
					.satisfies(e -> assertThat(
							((IncompleteJustificationException) e)
									.getIncompleteElementIds())
							.containsExactly("c1"));
		}

		@Test
		void lockThrowsWhenStrategyHasNoSupport() {
			conclusion.addSupport(strategy);
			Justification j = new Justification("j1");
			j.setConclusion(conclusion);
			j.addElement(strategy);
			assertThatThrownBy(j::lock)
					.isInstanceOf(IncompleteJustificationException.class)
					.satisfies(e -> assertThat(
							((IncompleteJustificationException) e)
									.getIncompleteElementIds())
							.containsExactly("s1"));
		}

		@Test
		void lockThrowsWhenSubConclusionHasNoSupport() {
			SubConclusion sc = new SubConclusion("sc1", "a sub-conclusion");
			strategy.addSupport(sc);
			conclusion.addSupport(strategy);
			Justification j = new Justification("j1");
			j.setConclusion(conclusion);
			j.addElement(strategy);
			j.addElement(sc);
			assertThatThrownBy(j::lock)
					.isInstanceOf(IncompleteJustificationException.class)
					.satisfies(e -> assertThat(
							((IncompleteJustificationException) e)
									.getIncompleteElementIds())
							.containsExactly("sc1"));
		}

		@Test
		void lockExceptionIncludesAllIncompleteIds() {
			Justification j = new Justification("j1");
			j.setConclusion(conclusion);
			j.addElement(strategy);
			assertThatThrownBy(j::lock)
					.isInstanceOf(IncompleteJustificationException.class)
					.satisfies(e -> assertThat(
							((IncompleteJustificationException) e)
									.getIncompleteElementIds())
							.containsExactlyInAnyOrder("c1", "s1"));
		}

		@Test
		void lockSetsLockedFlag() {
			Justification j = complete();
			j.lock();
			assertThat(j.isLocked()).isTrue();
		}

		@Test
		void addElementAfterLockThrows() {
			Justification j = complete();
			j.lock();
			assertThatThrownBy(() -> j.addElement(new Evidence("e2", "extra")))
					.isInstanceOf(LockedModelException.class);
		}

		@Test
		void lockExceptionReportsJustificationName() {
			Justification j = new Justification("myJustification");
			j.setConclusion(conclusion);
			assertThatThrownBy(j::lock)
					.isInstanceOf(IncompleteJustificationException.class)
					.satisfies(e -> assertThat(
							((IncompleteJustificationException) e)
									.getJustificationName())
							.isEqualTo("myJustification"));
		}
	}

	// -------------------------------------------------------------------------
	// Unit query methods
	// -------------------------------------------------------------------------

	@Nested
	class UnitQueries {

		private Unit unit;

		@BeforeEach
		void setUp() {
			unit = new Unit("src");
			unit.add(new Justification("j1"));
			unit.add(new Template("t1"));
		}

		@Test
		void templatesReturnsOnlyTemplates() {
			assertThat(unit.templates()).hasSize(1)
					.extracting(Template::getName).containsExactly("t1");
		}

		@Test
		void getThrowsForUnknownName() {
			assertThatThrownBy(() -> unit.get("nonexistent"))
					.isInstanceOf(NoSuchElementException.class)
					.hasMessageContaining("nonexistent");
		}
	}
}
