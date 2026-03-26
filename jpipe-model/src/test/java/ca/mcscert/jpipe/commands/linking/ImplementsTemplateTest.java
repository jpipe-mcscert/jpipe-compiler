package ca.mcscert.jpipe.commands.linking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
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
	// toString
	// -------------------------------------------------------------------------

	@Test
	void toStringIsPrologFact() {
		assertThat(new ImplementsTemplate("j1", "t1").toString()).isEqualTo("implements('j1', 't1').");
	}
}
