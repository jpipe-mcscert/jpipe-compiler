package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ActionListInterpretationTest {

	private ActionListInterpretation step;
	private CompilationContext ctx;

	@BeforeEach
	void setUp() {
		step = new ActionListInterpretation();
		ctx = new CompilationContext("test.jd");
	}

	@Test
	void emptyCommandListProducesEmptyUnit() throws Exception {
		Unit unit = step.run(List.of(), ctx);
		assertThat(unit.getModels()).isEmpty();
	}

	@Test
	void unitSourceMatchesContextSourcePath() throws Exception {
		Unit unit = step.run(List.of(), ctx);
		assertThat(unit.getSource()).isEqualTo("test.jd");
	}

	@Test
	void commandsAreExecuted() throws Exception {
		Unit unit = step.run(List.of(new CreateJustification("j1")), ctx);
		assertThat(unit.findModel("j1")).isPresent();
	}

	@Test
	void multipleCommandsAreAllExecuted() throws Exception {
		Unit unit = step.run(List.of(new CreateJustification("j1"), new CreateJustification("j2")), ctx);
		assertThat(unit.justifications()).extracting(j -> j.getName()).containsExactlyInAnyOrder("j1", "j2");
	}
}
