package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutedAction;
import ca.mcscert.jpipe.commands.MacroCommand;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class DiagnosticReportTest {

	private DiagnosticReport step;
	private CompilationContext ctx;
	private Unit unit;

	@BeforeEach
	void setUp() {
		step = new DiagnosticReport();
		ctx = new CompilationContext("test.jd");
		unit = new Unit("test.jd");
	}

	// -------------------------------------------------------------------------
	// Executed Actions section
	// -------------------------------------------------------------------------

	@Nested
	class ExecutedActionsSection {

		@Test
		void sectionIsAbsentWhenNoActionsWereRecorded() throws Exception {
			String report = step.run(unit, ctx);
			assertThat(report).doesNotContain("Executed Actions");
		}

		@Test
		void sectionAppearsWhenActionsArePresent() throws Exception {
			ctx.recordActions(
					List.of(action(new CreateJustification("j1"), 0)));
			String report = step.run(unit, ctx);
			assertThat(report).contains("=== Executed Actions ===");
		}

		@Test
		void regularActionIsNumberedAtBaseIndent() throws Exception {
			ctx.recordActions(
					List.of(action(new CreateJustification("j1"), 0)));
			String report = step.run(unit, ctx);
			assertThat(report).contains("  1. create_justification('j1').");
		}

		@Test
		void actionsAreNumberedSequentially() throws Exception {
			ctx.recordActions(List.of(action(new CreateJustification("j1"), 0),
					action(new CreateJustification("j2"), 0)));
			String report = step.run(unit, ctx);
			assertThat(report).contains("  1.").contains("  2.");
		}

		@Test
		void macroActionIsLabelledWithMacroPrefix() throws Exception {
			ctx.recordActions(List.of(action(macro("my_macro"), 0)));
			String report = step.run(unit, ctx);
			assertThat(report).contains("[macro] my_macro");
		}

		@Test
		void depth1ActionIsIndentedBeyondBaseLevel() throws Exception {
			ctx.recordActions(
					List.of(action(new CreateJustification("j1"), 1)));
			String report = step.run(unit, ctx);
			assertThat(report).contains("  1.   create_justification('j1').");
		}

		@Test
		void depth2ActionIsIndentedMoreThanDepth1() throws Exception {
			ctx.recordActions(List.of(action(new CreateJustification("j1"), 1),
					action(new CreateJustification("j2"), 2)));
			String report = step.run(unit, ctx);
			assertThat(report).containsSubsequence(
					"  1.   create_justification('j1').",
					"  2.     create_justification('j2').");
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static ExecutedAction action(Command command, int depth) {
		return new ExecutedAction(command, depth);
	}

	private static MacroCommand macro(String label) {
		return new MacroCommand() {
			@Override
			public List<Command> expand(ca.mcscert.jpipe.model.Unit context) {
				return List.of();
			}

			@Override
			public String toString() {
				return label;
			}
		};
	}
}
