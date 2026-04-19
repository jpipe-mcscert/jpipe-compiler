package ca.mcscert.jpipe.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
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
			Unit unit = engine.spawn("my.jd",
					List.of(new CreateJustification("j1")));
			assertThat(unit.findModel("j1")).isPresent();
		}

		@Test
		void executesMultipleCommandsInOrder() {
			Unit unit = engine.spawn("my.jd",
					List.of(new CreateJustification("j1"),
							new CreateJustification("j2")));
			assertThat(unit.justifications()).extracting(j -> j.getName())
					.containsExactly("j1", "j2");
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

			Unit unit = engine.spawn("src",
					List.of(conditionedCmd, new CreateJustification("j1")));
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
		void engineThrowsWhenAllCommandsAreStuck() {
			RegularCommand stuck1 = new RegularCommand() {
				@Override
				public Predicate<Unit> condition() {
					return unit -> false;
				}

				@Override
				public void doExecute(Unit context) {
					// intentionally empty
				}
			};
			RegularCommand stuck2 = new RegularCommand() {
				@Override
				public Predicate<Unit> condition() {
					return unit -> false;
				}

				@Override
				public void doExecute(Unit context) {
					// intentionally empty
				}
			};

			assertThatThrownBy(
					() -> engine.spawn("src", List.of(stuck1, stuck2)))
					.isInstanceOf(
							ca.mcscert.jpipe.commands.DeadlockException.class)
					.hasMessageContaining("deadlocked").satisfies(ex -> {
						var deadlock = (ca.mcscert.jpipe.commands.DeadlockException) ex;
						assertThat(deadlock.stuckCommands()).hasSize(2);
						assertThat(deadlock.partialUnit()).isNotNull();
					});
		}
	}

	// -------------------------------------------------------------------------
	// Execution history
	// -------------------------------------------------------------------------

	@Nested
	class ExecutionHistory {

		@Test
		void emptyCommandListProducesEmptyHistory() {
			engine.spawn("src", List.of());
			assertThat(engine.executedCommands()).isEmpty();
		}

		@Test
		void regularCommandsAreRecordedAtDepthZero() {
			engine.spawn("src", List.of(new CreateJustification("j1"),
					new CreateJustification("j2")));
			assertThat(engine.executedCommands()).hasSize(2)
					.allMatch(a -> a.depth() == 0).allMatch(a -> !a.isMacro());
		}

		@Test
		void regularCommandsAreRecordedInExecutionOrder() {
			engine.spawn("src", List.of(new CreateJustification("j1"),
					new CreateJustification("j2")));
			assertThat(engine.executedCommands())
					.extracting(a -> a.command().toString())
					.containsExactly("create_justification('j1').",
							"create_justification('j2').");
		}

		@Test
		void deferredCommandAppearsOnceInHistory() {
			RegularCommand deferred = new RegularCommand() {
				@Override
				public Predicate<Unit> condition() {
					return unit -> unit.findModel("j1").isPresent();
				}

				@Override
				public void doExecute(Unit context) {
					context.add(new ca.mcscert.jpipe.model.Justification("j2"));
				}
			};
			engine.spawn("src",
					List.of(deferred, new CreateJustification("j1")));
			assertThat(engine.executedCommands()).hasSize(2);
		}

		@Test
		void macroIsRecordedAtDepthZeroWithMacroFlag() {
			engine.spawn("src", List.of(new CreateTemplate("t"),
					new CreateConclusion("t", "tc", "Template conclusion"),
					new CreateAbstractSupport("t", "as", "Abstract support"),
					new CreateStrategy("t", "ts", "Template strategy"),
					new AddSupport("t", "tc", "ts"),
					new AddSupport("t", "ts", "as"),
					new CreateJustification("j"),
					new CreateConclusion("j", "c", "Conclusion"),
					new ImplementsTemplate("j", "t"),
					new OverrideAbstractSupport("j", "t:as", "evidence",
							"Concrete evidence")));
			assertThat(engine.executedCommands())
					.filteredOn(ExecutedAction::isMacro).hasSize(1)
					.allMatch(a -> a.depth() == 0);
		}

		@Test
		void macroExpansionCommandsAreRecordedAtDepthOne() {
			engine.spawn("src", List.of(new CreateTemplate("t"),
					new CreateConclusion("t", "tc", "Template conclusion"),
					new CreateAbstractSupport("t", "as", "Abstract support"),
					new CreateStrategy("t", "ts", "Template strategy"),
					new AddSupport("t", "tc", "ts"),
					new AddSupport("t", "ts", "as"),
					new CreateJustification("j"),
					new CreateConclusion("j", "c", "Conclusion"),
					new ImplementsTemplate("j", "t"),
					new OverrideAbstractSupport("j", "t:as", "evidence",
							"Concrete evidence")));
			assertThat(engine.executedCommands()).filteredOn(a -> !a.isMacro())
					.filteredOn(a -> a.depth() == 1).isNotEmpty();
		}
	}

	// -------------------------------------------------------------------------
	// Error propagation
	// -------------------------------------------------------------------------

	@Nested
	class ErrorPropagation {

		@Test
		void enginePropagatesExceptionFromCommand() {
			RegularCommand failing = new RegularCommand() {
				@Override
				public void doExecute(Unit context) {
					throw new IllegalStateException("intentional failure");
				}
			};

			List<Command> cmds = List.of(failing);
			assertThatThrownBy(() -> engine.spawn("src", cmds)).isInstanceOf(
					ca.mcscert.jpipe.commands.CommandExecutionException.class)
					.hasMessageContaining("intentional failure")
					.satisfies(ex -> {
						var cee = (ca.mcscert.jpipe.commands.CommandExecutionException) ex;
						assertThat(cee.failedCommand()).isSameAs(failing);
						assertThat(cee.partialUnit()).isNotNull();
					});
		}
	}
}
