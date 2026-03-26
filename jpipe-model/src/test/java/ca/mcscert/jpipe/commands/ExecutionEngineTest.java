package ca.mcscert.jpipe.commands;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExecutionEngineTest {

	private ExecutionEngine engine;

	@BeforeEach
	void setUp() {
		engine = new ExecutionEngine();
	}

	// -------------------------------------------------------------------------
	// spawn
	// -------------------------------------------------------------------------

	@Nested
	class Spawn {

		@Test
		void returnsUnitWithGivenSource() {
			Unit unit = engine.spawn("my.jd", List.of());
			assertThat(unit.getSource()).isEqualTo("my.jd");
		}

		@Test
		void emptyCommandListProducesEmptyUnit() {
			Unit unit = engine.spawn("my.jd", List.of());
			assertThat(unit.getModels()).isEmpty();
		}

		@Test
		void executesCommands() {
			Unit unit = engine.spawn("my.jd", List.of(new CreateJustification("j1")));
			assertThat(unit.findModel("j1")).isPresent();
		}

		@Test
		void executesMultipleCommandsInOrder() {
			Unit unit = engine.spawn("my.jd", List.of(new CreateJustification("j1"), new CreateJustification("j2")));
			assertThat(unit.justifications()).extracting(j -> j.getName()).containsExactly("j1", "j2");
		}
	}

	// -------------------------------------------------------------------------
	// enrich
	// -------------------------------------------------------------------------

	@Nested
	class Enrich {

		@Test
		void returnsTheSameUnitInstance() {
			Unit existing = new Unit("existing.jd");
			Unit result = engine.enrich(existing, List.of());
			assertThat(result).isSameAs(existing);
		}

		@Test
		void appendsToExistingUnit() {
			Unit existing = new Unit("existing.jd");
			existing.add(new ca.mcscert.jpipe.model.Justification("j1"));
			engine.enrich(existing, List.of(new CreateJustification("j2")));
			assertThat(existing.getModels()).hasSize(2);
		}
	}

	// -------------------------------------------------------------------------
	// Deferred execution
	// -------------------------------------------------------------------------

	@Nested
	class DeferredExecution {

		@Test
		void commandIsRetiredAfterItsConditionBecomesTrue() {
			RegularCommand conditionedCmd = new RegularCommand() {
				@Override
				public Predicate<Unit> condition() {
					return unit -> unit.findModel("j1").isPresent();
				}

				@Override
				public void doExecute(Unit context) {
					context.add(new ca.mcscert.jpipe.model.Justification("j2"));
				}
			};

			Unit unit = engine.spawn("src", List.of(conditionedCmd, new CreateJustification("j1")));
			assertThat(unit.findModel("j1")).isPresent();
			assertThat(unit.findModel("j2")).isPresent();
		}
	}

	// -------------------------------------------------------------------------
	// Deadlock detection
	// -------------------------------------------------------------------------

	@Nested
	class DeadlockDetection {

		@Test
		void engineTerminatesWhenAllCommandsAreStuck() {
			RegularCommand stuck1 = new RegularCommand() {
				@Override
				public Predicate<Unit> condition() {
					return unit -> false;
				}

				@Override
				public void doExecute(Unit context) {
				}
			};
			RegularCommand stuck2 = new RegularCommand() {
				@Override
				public Predicate<Unit> condition() {
					return unit -> false;
				}

				@Override
				public void doExecute(Unit context) {
				}
			};

			Unit unit = engine.spawn("src", List.of(stuck1, stuck2));
			assertThat(unit.getModels()).isEmpty();
		}
	}

	// -------------------------------------------------------------------------
	// Error recovery
	// -------------------------------------------------------------------------

	@Nested
	class ErrorRecovery {

		@Test
		void engineContinuesAfterExceptionInExecute() {
			RegularCommand failing = new RegularCommand() {
				@Override
				public void doExecute(Unit context) throws Exception {
					throw new RuntimeException("intentional failure");
				}
			};

			Unit unit = engine.spawn("src", List.of(failing, new CreateJustification("j1")));
			assertThat(unit.findModel("j1")).isPresent();
		}
	}
}
