package ca.mcscert.jpipe.commands.linking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AddSupportTest {

	// -------------------------------------------------------------------------
	// Valid support pairs
	// -------------------------------------------------------------------------

	@Nested
	class ConclusionSupportedByStrategy {

		@Test
		void strategyIsLinkedToConclusion() {
			Unit unit = unitWithJustification("j1");
			new CreateConclusion("j1", "c1", "my conclusion").execute(unit);
			new CreateStrategy("j1", "s1", "my strategy").execute(unit);

			new AddSupport("j1", "c1", "s1").execute(unit);

			Conclusion c = conclusion(unit, "j1");
			assertThat(c.getSupport()).isPresent().get()
					.extracting(Strategy::id).isEqualTo("s1");
		}
	}

	@Nested
	class SubConclusionSupportedByStrategy {

		@Test
		void strategyIsLinkedToSubConclusion() {
			Unit unit = unitWithJustification("j1");
			new CreateSubConclusion("j1", "sc1", "sub").execute(unit);
			new CreateStrategy("j1", "s1", "strategy").execute(unit);

			new AddSupport("j1", "sc1", "s1").execute(unit);

			SubConclusion sc = element(unit, "j1", "sc1", SubConclusion.class);
			assertThat(sc.getSupport()).isPresent().get()
					.extracting(Strategy::id).isEqualTo("s1");
		}
	}

	@Nested
	class StrategySupportedByEvidence {

		@Test
		void evidenceIsLinkedToStrategy() {
			Unit unit = unitWithJustification("j1");
			new CreateStrategy("j1", "s1", "strategy").execute(unit);
			new CreateEvidence("j1", "e1", "evidence").execute(unit);

			new AddSupport("j1", "s1", "e1").execute(unit);

			Strategy s = element(unit, "j1", "s1", Strategy.class);
			assertThat(s.getSupports())
					.extracting(sl -> ((JustificationElement) sl).id())
					.contains("e1");
		}
	}

	@Nested
	class StrategySupportedBySubConclusion {

		@Test
		void subConclusionIsLinkedToStrategyAsSupportLeaf() {
			Unit unit = unitWithJustification("j1");
			new CreateStrategy("j1", "s1", "strategy").execute(unit);
			new CreateSubConclusion("j1", "sc1", "sub").execute(unit);

			new AddSupport("j1", "s1", "sc1").execute(unit);

			Strategy s = element(unit, "j1", "s1", Strategy.class);
			assertThat(s.getSupports())
					.extracting(sl -> ((JustificationElement) sl).id())
					.contains("sc1");
		}
	}

	@Nested
	class StrategySupportedByAbstractSupport {

		@Test
		void abstractSupportIsLinkedToStrategyInTemplate() {
			Unit unit = new Unit("src");
			new CreateTemplate("t1").execute(unit);
			new CreateStrategy("t1", "s1", "strategy").execute(unit);
			new CreateAbstractSupport("t1", "as1", "abstract").execute(unit);

			new AddSupport("t1", "s1", "as1").execute(unit);

			Strategy s = element(unit, "t1", "s1", Strategy.class);
			assertThat(s.getSupports())
					.extracting(sl -> ((JustificationElement) sl).id())
					.contains("as1");
		}
	}

	// -------------------------------------------------------------------------
	// Invalid support pair
	// -------------------------------------------------------------------------

	@Nested
	class InvalidSupportPair {

		@Test
		void evidenceCannotBeSupportable() {
			Unit unit = unitWithJustification("j1");
			new CreateEvidence("j1", "e1", "evidence").execute(unit);
			new CreateStrategy("j1", "s1", "strategy").execute(unit);

			var cmd = new AddSupport("j1", "e1", "s1");
			assertThatThrownBy(() -> cmd.execute(unit))
					.isInstanceOf(IllegalArgumentException.class);
		}
	}

	// -------------------------------------------------------------------------
	// Template encapsulation — an implementor may only override @support
	// placeholders, never reference the parent template's own structure.
	// -------------------------------------------------------------------------

	@Nested
	class TemplateEncapsulation {

		/**
		 * Every reference to the parent template's own strategy {@code t:s} is
		 * rejected, whether it appears as the supportable or the supporter, and
		 * whether it is written qualified ({@code t:s}) or as a plain id
		 * ({@code s}) — {@code findById} resolves the plain id, so the
		 * restriction must apply to the resolved id.
		 */
		@ParameterizedTest
		@CsvSource({"t:s, local", "s, local", "local, t:s"})
		void rejectsReferenceToTemplateInternalStructure(String supportable,
				String supporter) {
			Unit unit = unitWithImplementation();
			var cmd = new AddSupport("j", supportable, supporter);

			assertThatThrownBy(() -> cmd.execute(unit))
					.isInstanceOf(ReferenceIntoTemplateException.class)
					.hasMessageContaining("t:s");
		}

		/**
		 * A unit with template {@code t} (conclusion c, strategy s, @support
		 * abs) and a justification {@code j} that inlines it and adds a local
		 * evidence.
		 */
		private static Unit unitWithImplementation() {
			Unit unit = new Unit("src");
			Template t = new Template("t");
			Conclusion tc = new Conclusion("c", "conclusion");
			t.setConclusion(tc);
			Strategy ts = new Strategy("s", "strategy");
			t.addElement(ts);
			AbstractSupport abs = new AbstractSupport("abs", "abstract");
			t.addElement(abs);
			ts.addSupport(abs);
			tc.addSupport(ts);
			unit.add(t);

			Justification j = new Justification("j");
			j.inline(t, "t");
			j.addElement(new Evidence("local", "local evidence"));
			unit.add(j);
			return unit;
		}
	}

	// -------------------------------------------------------------------------
	// Condition / deferred execution
	// -------------------------------------------------------------------------

	@Nested
	class DeferredExecution {

		@Test
		void conditionFalseWhenModelAbsent() {
			Unit unit = new Unit("src");
			assertThat(new AddSupport("j1", "c1", "s1").condition().test(unit))
					.isFalse();
		}

		@Test
		void conditionFalseWhenOnlyOnElementExists() {
			Unit unit = unitWithJustification("j1");
			new CreateConclusion("j1", "c1", "conclusion").execute(unit);
			assertThat(new AddSupport("j1", "c1", "s1").condition().test(unit))
					.isFalse();
		}

		@Test
		void conditionTrueWhenBothElementsExist() {
			Unit unit = unitWithJustification("j1");
			new CreateConclusion("j1", "c1", "conclusion").execute(unit);
			new CreateStrategy("j1", "s1", "strategy").execute(unit);
			assertThat(new AddSupport("j1", "c1", "s1").condition().test(unit))
					.isTrue();
		}

		@Test
		void engineDefersUntilBothElementsCreated() {
			ExecutionEngine engine = new ExecutionEngine();
			Unit unit = engine.spawn("src",
					List.of(new AddSupport("j1", "c1", "s1"),
							new CreateJustification("j1"),
							new CreateConclusion("j1", "c1", "conclusion"),
							new CreateStrategy("j1", "s1", "strategy")));

			Conclusion c = conclusion(unit, "j1");
			assertThat(c.getSupport()).isPresent().get()
					.extracting(Strategy::id).isEqualTo("s1");
		}
	}

	// -------------------------------------------------------------------------
	// toString
	// -------------------------------------------------------------------------

	@Test
	void toStringIsPrologFact() {
		assertThat(new AddSupport("j1", "c1", "s1"))
				.hasToString("support('j1', 'c1', 's1').");
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static Unit unitWithJustification(String name) {
		Unit unit = new Unit("src");
		unit.add(new Justification(name));
		return unit;
	}

	private static Conclusion conclusion(Unit unit, String model) {
		return unit.get(model).conclusion().orElseThrow(
				() -> new AssertionError("No conclusion in " + model));
	}

	private static <T> T element(Unit unit, String model, String id,
			Class<T> type) {
		return unit.get(model).findById(id).map(type::cast).orElseThrow(
				() -> new AssertionError("No element " + id + " in " + model));
	}
}
