package ca.mcscert.jpipe.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UnitAliasTest {

	private Unit unit;

	@BeforeEach
	void setUp() {
		unit = new Unit("src");
	}

	@Nested
	class ResolveAlias {

		@Test
		void returnsIdUnchangedWhenNoAliasRecorded() {
			assertThat(unit.resolveAlias("result", "a:elem"))
					.isEqualTo("a:elem");
		}

		@Test
		void returnsNewIdAfterRecording() {
			unit.recordAlias("result", "a:elem", "elem");
			assertThat(unit.resolveAlias("result", "a:elem")).isEqualTo("elem");
		}

		@Test
		void aliasIsScopedToModel() {
			unit.recordAlias("result", "a:elem", "elem");
			assertThat(unit.resolveAlias("other", "a:elem"))
					.isEqualTo("a:elem");
		}

		@Test
		void multipleAliasesInSameModel() {
			unit.recordAlias("result", "a:s", "s");
			unit.recordAlias("result", "b:s", "s");
			assertThat(unit.resolveAlias("result", "a:s")).isEqualTo("s");
			assertThat(unit.resolveAlias("result", "b:s")).isEqualTo("s");
		}
	}
}
