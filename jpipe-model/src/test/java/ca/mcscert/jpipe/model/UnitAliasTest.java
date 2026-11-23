package ca.mcscert.jpipe.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UnitAliasTest {

	private Unit unit;
	private Justification model;

	@BeforeEach
	void setUp() {
		unit = new Unit("src");
		model = new Justification("result");
		unit.add(model);
	}

	@Test
	void recordingAnAliasRecordsItOnTheModel() {
		unit.recordAlias("result", "a:s", "s");

		assertThat(model.aliases()).containsEntry("a:s", "s");
	}

	@Test
	void anAliasIsScopedToItsModel() {
		Justification other = new Justification("other");
		unit.add(other);

		unit.recordAlias("result", "a:elem", "elem");

		assertThat(other.resolveAlias("a:elem")).isEqualTo("a:elem");
	}

	@Test
	void anAliasForAModelNotYetCreatedIsRefused() {
		assertThatThrownBy(() -> unit.recordAlias("absent", "a:s", "s"))
				.isInstanceOf(NoSuchElementException.class)
				.hasMessageContaining("absent");
	}
}
