package ca.mcscert.jpipe.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import java.util.List;
import org.junit.jupiter.api.Test;

class MacroCommandTest {

	@Test
	void execute_throws_unsupported_operation() {
		MacroCommand macro = unit -> List.of();
		Unit unit = new Unit("src");
		assertThatThrownBy(() -> macro.execute(unit))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void expand_returns_commands_to_execute() {
		Unit unit = new Unit("src");
		Command inner = new CreateEvidence("j", "e1", "Evidence");
		MacroCommand macro = u -> List.of(inner);

		List<Command> expanded = macro.expand(unit);

		assertThat(expanded).containsExactly(inner);
	}

	@Test
	void condition_defaults_to_always_true() {
		MacroCommand macro = unit -> List.of();
		assertThat(macro.condition().test(new Unit("src"))).isTrue();
	}
}
