package ca.mcscert.jpipe.commands.linking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AddSupportTest {

	// -------------------------------------------------------------------------
	// Valid support pairs
	// -------------------------------------------------------------------------

	@Nested
	class ConclusionSupportedByStrategy {

		@Test
		void strategyIsLinkedToConclusion() throws Exception {
			Unit unit = unitWithJustification("j1");
			new CreateConclusion("j1", "c1", "my conclusion").execute(unit);
			new CreateStrategy("j1", "s1", "my strategy").execute(unit);

			new AddSupport("j1", "c1", "s1").execute(unit);

			Conclusion c = conclusion(unit, "j1");
			assertThat(c.getSupport()).isPresent().get().extracting(Strategy::id).isEqualTo("s1");
		}
	}

	@Nested
	class SubConclusionSupportedByStrategy {

		@Test
		void strategyIsLinkedToSubConclusion() throws Exception {
			Unit unit = unitWithJustification("j1");
			new CreateSubConclusion("j1", "sc1", "sub").execute(unit);
			new CreateStrategy("j1", "s1", "strategy").execute(unit);

			new AddSupport("j1", "sc1", "s1").execute(unit);

			SubConclusion sc = element(unit, "j1", "sc1", SubConclusion.class);
			assertThat(sc.getSupport()).isPresent().get().extracting(Strategy::id).isEqualTo("s1");
		}
	}

	@Nested
	class StrategySupportedByEvidence {

		@Test
		void evidenceIsLinkedToStrategy() throws Exception {
			Unit unit = unitWithJustification("j1");
			new CreateStrategy("j1", "s1", "strategy").execute(unit);
			new CreateEvidence("j1", "e1", "evidence").execute(unit);

			new AddSupport("j1", "s1", "e1").execute(unit);

			Strategy s = element(unit, "j1", "s1", Strategy.class);
			assertThat(s.getSupport()).isPresent().get().isInstanceOf(Evidence.class)
					.extracting(sl -> ((Evidence) sl).id()).isEqualTo("e1");
		}
	}

	@Nested
	class StrategySupportedBySubConclusion {

		@Test
		void subConclusionIsLinkedToStrategyAsSupportLeaf() throws Exception {
			Unit unit = unitWithJustification("j1");
			new CreateStrategy("j1", "s1", "strategy").execute(unit);
			new CreateSubConclusion("j1", "sc1", "sub").execute(unit);

			new AddSupport("j1", "s1", "sc1").execute(unit);

			Strategy s = element(unit, "j1", "s1", Strategy.class);
			assertThat(s.getSupport()).isPresent().get().isInstanceOf(SubConclusion.class)
					.extracting(sl -> ((SubConclusion) sl).id()).isEqualTo("sc1");
		}
	}

	@Nested
	class StrategySupportedByAbstractSupport {

		@Test
		void abstractSupportIsLinkedToStrategyInTemplate() throws Exception {
			Unit unit = new Unit("src");
			new CreateTemplate("t1").execute(unit);
			new CreateStrategy("t1", "s1", "strategy").execute(unit);
			new CreateAbstractSupport("t1", "as1", "abstract").execute(unit);

			new AddSupport("t1", "s1", "as1").execute(unit);

			Strategy s = element(unit, "t1", "s1", Strategy.class);
			assertThat(s.getSupport()).isPresent().get().isInstanceOf(AbstractSupport.class)
					.extracting(sl -> ((AbstractSupport) sl).id()).isEqualTo("as1");
		}
	}

	// -------------------------------------------------------------------------
	// Invalid support pair
	// -------------------------------------------------------------------------

	@Nested
	class InvalidSupportPair {

		@Test
		void evidenceCannotBeSupportable() throws Exception {
			Unit unit = unitWithJustification("j1");
			new CreateEvidence("j1", "e1", "evidence").execute(unit);
			new CreateStrategy("j1", "s1", "strategy").execute(unit);

			assertThatThrownBy(() -> new AddSupport("j1", "e1", "s1").execute(unit))
					.isInstanceOf(IllegalArgumentException.class);
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
			assertThat(new AddSupport("j1", "c1", "s1").condition().test(unit)).isFalse();
		}

		@Test
		void conditionFalseWhenOnlyOnElementExists() throws Exception {
			Unit unit = unitWithJustification("j1");
			new CreateConclusion("j1", "c1", "conclusion").execute(unit);
			assertThat(new AddSupport("j1", "c1", "s1").condition().test(unit)).isFalse();
		}

		@Test
		void conditionTrueWhenBothElementsExist() throws Exception {
			Unit unit = unitWithJustification("j1");
			new CreateConclusion("j1", "c1", "conclusion").execute(unit);
			new CreateStrategy("j1", "s1", "strategy").execute(unit);
			assertThat(new AddSupport("j1", "c1", "s1").condition().test(unit)).isTrue();
		}

		@Test
		void engineDefersUntilBothElementsCreated() {
			ExecutionEngine engine = new ExecutionEngine();
			Unit unit = engine.spawn("src", List.of(new AddSupport("j1", "c1", "s1"), new CreateJustification("j1"),
					new CreateConclusion("j1", "c1", "conclusion"), new CreateStrategy("j1", "s1", "strategy")));

			Conclusion c = conclusion(unit, "j1");
			assertThat(c.getSupport()).isPresent().get().extracting(Strategy::id).isEqualTo("s1");
		}
	}

	// -------------------------------------------------------------------------
	// toString
	// -------------------------------------------------------------------------

	@Test
	void toStringIsPrologFact() {
		assertThat(new AddSupport("j1", "c1", "s1").toString()).isEqualTo("support('j1', 'c1', 's1').");
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
		return unit.get(model).conclusion().orElseThrow(() -> new AssertionError("No conclusion in " + model));
	}

	private static <T> T element(Unit unit, String model, String id, Class<T> type) {
		return unit.get(model).findById(id).map(type::cast)
				.orElseThrow(() -> new AssertionError("No element " + id + " in " + model));
	}
}
