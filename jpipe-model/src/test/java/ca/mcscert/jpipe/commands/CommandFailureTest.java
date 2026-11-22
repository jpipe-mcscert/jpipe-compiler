package ca.mcscert.jpipe.commands;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CommandFailureTest {

	private Unit unit;

	@BeforeEach
	void setUp() {
		unit = new Unit("src");
		unit.add(new Justification("j"));
		unit.add(new Template("t"));
	}

	@Test
	void aFailedImplementsLeavesItsModelIncomplete() {
		Command implementsT = new ImplementsTemplate("j", "t");

		implementsT.recordFailure(unit);

		assertThat(implementsT.breaksItsTarget()).isTrue();
		assertThat(unit.isComplete("j")).isFalse();
	}

	@Test
	void aFailedRelationLeavesItsModelToBeChecked() {
		Command support = new AddSupport("j", "c", "s");

		support.recordFailure(unit);

		assertThat(support.breaksItsTarget()).isFalse();
		assertThat(unit.isComplete("j")).isTrue();
	}

	@Test
	void aFailedOverrideRecordsThePlaceholderItLeaves() {
		unit.get("j").addElement(new AbstractSupport("t:abs", "A"));
		Command override = new OverrideAbstractSupport("j", "abs", "evidence",
				"E");

		override.recordFailure(unit);

		assertThat(unit.overrideFailed("j", "t:abs")).isTrue();
		assertThat(unit.isComplete("j")).isTrue();
	}

	@Test
	void anImplementsReadsItsTemplate() {
		assertThat(new ImplementsTemplate("j", "t").reads())
				.containsExactly("t");
	}

	@Test
	void modelsAreSettledWhenTheyExistAreCompleteAndNothingChangesThem() {
		PendingCommands changingT = (model, except) -> model.equals("t");

		assertThat(PendingCommands.NONE.settled(unit, Set.of("j", "t")))
				.isTrue();
		assertThat(changingT.settled(unit, Set.of("t"))).isFalse();
		assertThat(PendingCommands.NONE.settled(unit, Set.of("absent")))
				.isFalse();
		unit.markIncomplete("t");
		assertThat(PendingCommands.NONE.settled(unit, Set.of("t"))).isFalse();
	}
}
