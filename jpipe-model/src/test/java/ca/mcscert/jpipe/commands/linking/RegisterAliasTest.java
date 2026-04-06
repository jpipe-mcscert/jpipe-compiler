package ca.mcscert.jpipe.commands.linking;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class RegisterAliasTest {

	private Unit unit;

	@BeforeEach
	void setUp() {
		unit = new Unit("src");
	}

	@Nested
	class Execute {

		@Test
		void recordsAliasInUnit() throws Exception {
			new RegisterAlias("result", "a:elem", "elem").execute(unit);
			assertThat(unit.resolveAlias("result", "a:elem")).isEqualTo("elem");
		}

		@Test
		void doesNotAffectOtherModels() throws Exception {
			new RegisterAlias("result", "a:elem", "elem").execute(unit);
			assertThat(unit.resolveAlias("other", "a:elem"))
					.isEqualTo("a:elem");
		}

		@Test
		void conditionIsAlwaysTrue() {
			assertThat(new RegisterAlias("result", "a:elem", "elem").condition()
					.test(unit)).isTrue();
		}
	}

	@Nested
	class Accessors {

		@Test
		void containerReturnsConstructorValue() {
			assertThat(new RegisterAlias("r", "old", "new").container())
					.isEqualTo("r");
		}

		@Test
		void oldIdReturnsConstructorValue() {
			assertThat(new RegisterAlias("r", "old", "new").oldId())
					.isEqualTo("old");
		}

		@Test
		void newIdReturnsConstructorValue() {
			assertThat(new RegisterAlias("r", "old", "new").newId())
					.isEqualTo("new");
		}
	}

	@Nested
	class ToStringRepresentation {

		@Test
		void containsAllThreeParts() {
			String s = new RegisterAlias("result", "a:elem", "elem").toString();
			assertThat(s).contains("result").contains("a:elem")
					.contains("elem");
		}
	}
}
