package ca.mcscert.jpipe.operators;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AliasRegistryTest {

	private AliasRegistry registry;

	@BeforeEach
	void setUp() {
		registry = new AliasRegistry();
	}

	@Nested
	class Resolve {

		@Test
		void returnsIdUnchangedWhenNoAliasRegistered() {
			assertThat(registry.resolve("a:elem")).isEqualTo("a:elem");
		}

		@Test
		void returnsNewIdAfterSingleRegister() {
			registry.register("merged", "a:elem");
			assertThat(registry.resolve("a:elem")).isEqualTo("merged");
		}

		@Test
		void returnsNewIdForAllOldIdsAfterListRegister() {
			registry.register("merged", List.of("a:elem", "b:elem"));
			assertThat(registry.resolve("a:elem")).isEqualTo("merged");
			assertThat(registry.resolve("b:elem")).isEqualTo("merged");
		}

		@Test
		void returnsNewIdUnchangedWhenItIsNotItself() {
			registry.register("merged", "a:elem");
			assertThat(registry.resolve("merged")).isEqualTo("merged");
		}
	}

	@Nested
	class Aliases {

		@Test
		void emptyWhenNoRegistrations() {
			assertThat(registry.aliases()).isEmpty();
		}

		@Test
		void containsAllRegisteredMappings() {
			registry.register("m", List.of("a:s", "b:s"));
			assertThat(registry.aliases()).containsEntry("a:s", "m")
					.containsEntry("b:s", "m").hasSize(2);
		}

		@Test
		void isUnmodifiable() {
			assertThat(registry.aliases()).isUnmodifiable();
		}
	}

	@Nested
	class Register {

		@Test
		void laterRegistrationOverwritesPrevious() {
			registry.register("first", "x");
			registry.register("second", "x");
			assertThat(registry.resolve("x")).isEqualTo("second");
		}
	}
}
