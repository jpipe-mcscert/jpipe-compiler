package ca.mcscert.jpipe.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;
import java.util.ArrayList;
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
			existing.add(new Justification("j1"));
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
					context.add(new Justification("j2"));
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
					context.add(new Justification("j2"));
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

		@Test
		void a_failure_handler_records_the_failure_and_the_queue_goes_on() {
			// The compiler reports every error of a source: one failing
			// command must not hide the commands after it (#183).
			RegularCommand failing = new RegularCommand() {
				@Override
				public void doExecute(Unit context) {
					throw new IllegalStateException("intentional failure");
				}
			};
			List<CommandExecutionException> failures = new ArrayList<>();
			ExecutionEngine collecting = new ExecutionEngine(failures::add);

			Unit unit = collecting.spawn("src",
					List.of(failing, new CreateJustification("after")));

			assertThat(failures).singleElement().satisfies(
					f -> assertThat(f.failedCommand()).isSameAs(failing));
			assertThat(unit.findModel("after")).isPresent();
		}
	}

	@Nested
	class DeclarationOrder {

		@Test
		void implements_waits_for_the_template_to_be_complete() {
			// The template's relation comes after the implements: it used to
			// be copied without it (#177).
			Unit unit = engine.spawn("src",
					List.of(new CreateTemplate("t"),
							new CreateJustification("j"),
							new ImplementsTemplate("j", "t"),
							new CreateConclusion("t", "c", "C"),
							new CreateStrategy("t", "s", "S"),
							new CreateAbstractSupport("t", "abs", "A"),
							new AddSupport("t", "c", "s"),
							new AddSupport("t", "s", "abs")));

			var j = unit.get("j");
			assertThat(j.conclusion()).hasValueSatisfying(
					c -> assertThat(c.getSupport()).isPresent());
			assertThat(j.findById("t:s")).hasValueSatisfying(
					s -> assertThat(((Strategy) s).getSupports())
							.extracting(e -> e.id()).containsExactly("t:abs"));
		}

		@Test
		void a_relation_binds_the_local_element_declared_after_it() {
			// A short id used to bind the inherited t:e, found first (#177).
			Unit unit = engine.spawn("src", List.of(new CreateTemplate("t"),
					new CreateConclusion("t", "c", "C"),
					new CreateStrategy("t", "s", "S"),
					new CreateAbstractSupport("t", "e", "E"),
					new AddSupport("t", "c", "s"),
					new AddSupport("t", "s", "e"), new CreateJustification("j"),
					new ImplementsTemplate("j", "t"),
					new CreateStrategy("j", "s2", "S2"),
					new AddSupport("j", "s2", "e"),
					new CreateEvidence("j", "e", "Local")));

			var s2 = (Strategy) unit.get("j").findById("s2").orElseThrow();
			assertThat(s2.getSupports()).extracting(e -> e.id())
					.containsExactly("e");
		}

		@Test
		void implements_waits_on_an_incomplete_template_forever() {
			Unit unit = new Unit("src");
			unit.add(new Template("t"));
			unit.add(new Justification("j"));
			unit.markIncomplete("t");
			List<Command> cmds = List.of(new ImplementsTemplate("j", "t"));

			assertThatThrownBy(() -> engine.enrich(unit, cmds))
					.isInstanceOf(DeadlockException.class);
		}
	}
}
