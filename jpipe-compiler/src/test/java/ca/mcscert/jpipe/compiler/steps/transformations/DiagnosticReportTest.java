package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_DEFERRALS;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_MACROS;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_TOTAL;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutedAction;
import ca.mcscert.jpipe.commands.MacroCommand;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
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
		void sectionIsAbsentWhenNoActionsWereRecorded() {
			String report = step.run(unit, ctx);
			assertThat(report).doesNotContain("Executed Actions");
		}

		@Test
		void sectionAppearsWhenActionsArePresent() {
			ctx.recordActions(
					List.of(action(new CreateJustification("j1"), 0)));
			String report = step.run(unit, ctx);
			assertThat(report).contains("=== Executed Actions ===");
		}

		@Test
		void regularActionIsNumberedAtBaseIndent() {
			ctx.recordActions(
					List.of(action(new CreateJustification("j1"), 0)));
			String report = step.run(unit, ctx);
			assertThat(report).contains("  1. create_justification('j1').");
		}

		@Test
		void actionsAreNumberedSequentially() {
			ctx.recordActions(List.of(action(new CreateJustification("j1"), 0),
					action(new CreateJustification("j2"), 0)));
			String report = step.run(unit, ctx);
			assertThat(report).contains("  1.").contains("  2.");
		}

		@Test
		void macroActionIsLabelledWithMacroPrefix() {
			ctx.recordActions(List.of(action(macro("my_macro"), 0)));
			String report = step.run(unit, ctx);
			assertThat(report).contains("[macro] my_macro");
		}

		@Test
		void depth1ActionIsIndentedBeyondBaseLevel() {
			ctx.recordActions(
					List.of(action(new CreateJustification("j1"), 1)));
			String report = step.run(unit, ctx);
			assertThat(report).contains("  1.   create_justification('j1').");
		}

		@Test
		void depth2ActionIsIndentedMoreThanDepth1() {
			ctx.recordActions(List.of(action(new CreateJustification("j1"), 1),
					action(new CreateJustification("j2"), 2)));
			String report = step.run(unit, ctx);
			assertThat(report).containsSubsequence(
					"  1.   create_justification('j1').",
					"  2.     create_justification('j2').");
		}
	}

	// -------------------------------------------------------------------------
	// Diagnostics section
	// -------------------------------------------------------------------------

	@Nested
	class DiagnosticsSection {

		@Test
		void noDiagnostics_prints_none() {
			String report = step.run(unit, ctx);
			assertThat(report).contains("=== Diagnostics ===")
					.contains("(none)");
		}

		@Test
		void errorWithoutLocation_prints_level_source_message() {
			ctx.error("something went wrong");
			String report = step.run(unit, ctx);
			assertThat(report).contains("[ERROR]")
					.contains("something went wrong");
		}

		@Test
		void errorWithLocation_includes_line_and_column() {
			ctx.error(5, 10, "bad token");
			String report = step.run(unit, ctx);
			assertThat(report).contains("5:10").contains("bad token");
		}

		@Test
		void markDiagnosticsRendered_is_called_after_run() {
			step.run(unit, ctx);
			assertThat(ctx.diagnosticsRendered()).isTrue();
		}
	}

	// -------------------------------------------------------------------------
	// Action Statistics section
	// -------------------------------------------------------------------------

	@Nested
	class ActionStatsSection {

		@Test
		void sectionIsAbsentWhenNoStatsRecorded() {
			String report = step.run(unit, ctx);
			assertThat(report).doesNotContain("Action Statistics");
		}

		@Test
		void sectionAppearsWhenStatsAreRecorded() {
			ctx.recordStat(STAT_COMMANDS_TOTAL, 3L);
			ctx.recordStat(STAT_COMMANDS_MACROS, 1L);
			ctx.recordStat(STAT_COMMANDS_DEFERRALS, 0L);
			String report = step.run(unit, ctx);
			assertThat(report).contains("=== Action Statistics ===");
		}

		@Test
		void commandCounts_are_formatted_correctly() {
			ctx.recordStat(STAT_COMMANDS_TOTAL, 5L);
			ctx.recordStat(STAT_COMMANDS_MACROS, 2L);
			ctx.recordStat(STAT_COMMANDS_DEFERRALS, 1L);
			String report = step.run(unit, ctx);
			assertThat(report).contains("commands: 5 total (2 macro)")
					.contains("deferrals: 1");
		}
	}

	// -------------------------------------------------------------------------
	// Model Summary section
	// -------------------------------------------------------------------------

	@Nested
	class ModelSummarySection {

		@Test
		void emptyUnit_shows_header_only() {
			String report = step.run(unit, ctx);
			assertThat(report).contains("=== Model Summary ===");
		}

		@Test
		void justification_shows_kind_and_name() {
			unit.add(new Justification("myJ"));
			String report = step.run(unit, ctx);
			assertThat(report).contains("justification \"myJ\"");
		}

		@Test
		void template_shows_kind_and_name() {
			unit.add(new Template("myT"));
			String report = step.run(unit, ctx);
			assertThat(report).contains("template \"myT\"");
		}

		@Test
		void model_with_conclusion_shows_element_summary() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "A conclusion"));
			unit.add(j);
			String report = step.run(unit, ctx);
			assertThat(report).contains("conclusion(1)");
		}

		@Test
		void model_with_multiple_elements_shows_all_counts() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "A conclusion"));
			j.addElement(new Strategy("s", "A strategy"));
			j.addElement(new Evidence("e", "An evidence"));
			unit.add(j);
			String report = step.run(unit, ctx);
			assertThat(report).contains("conclusion(1)").contains("strategy(1)")
					.contains("evidence(1)");
		}

		@Test
		void template_implemented_by_justification_shows_used_by() {
			Template t = new Template("base");
			unit.add(t);
			Justification j = new Justification("impl");
			j.inline(t, "base");
			unit.add(j);
			String report = step.run(unit, ctx);
			assertThat(report).contains("used by").contains("\"impl\"");
		}
	}

	// -------------------------------------------------------------------------
	// Symbol Table section
	// -------------------------------------------------------------------------

	@Nested
	class SymbolTableSection {

		@Test
		void emptyUnit_shows_empty_symbol_table() {
			String report = step.run(unit, ctx);
			assertThat(report).contains("=== Symbol Table ===")
					.contains("(empty)");
		}

		@Test
		void model_with_known_location_shows_location() {
			unit.add(new Justification("j"));
			unit.recordLocation("j", new SourceLocation("test.jd", 1, 1));
			String report = step.run(unit, ctx);
			assertThat(report).contains("justification \"j\"");
		}

		@Test
		void element_with_known_location_shows_line_column() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "A conclusion"));
			unit.add(j);
			unit.recordLocation("j", new SourceLocation("test.jd", 1, 1));
			unit.recordLocation("j", "c", new SourceLocation("test.jd", 2, 3));
			String report = step.run(unit, ctx);
			assertThat(report).contains("2:3");
		}

		@Test
		void alias_appears_in_symbol_table() {
			unit.add(new Justification("j"));
			unit.recordAlias("j", "oldId", "newId");
			unit.recordLocation("j", new SourceLocation("test.jd", 1, 1));
			String report = step.run(unit, ctx);
			assertThat(report).contains("[alias]");
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
