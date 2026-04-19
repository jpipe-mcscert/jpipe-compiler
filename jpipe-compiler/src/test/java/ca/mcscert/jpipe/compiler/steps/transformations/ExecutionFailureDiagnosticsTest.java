package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ExecutionFailureDiagnosticsTest {

	private CompilationContext ctx;
	private Unit unit;

	@BeforeEach
	void setUp() {
		ctx = new CompilationContext("test.jd");
		unit = new Unit("test.jd");
	}

	// -------------------------------------------------------------------------
	// diagnoseDeadlock
	// -------------------------------------------------------------------------

	@Nested
	class DiagnoseDeadlock {

		@Test
		void addSupport_with_unknown_model_emits_error() {
			AddSupport cmd = new AddSupport("missing", "c", "s");
			ExecutionFailureDiagnostics.diagnoseDeadlock(List.of(cmd), unit,
					ctx);
			assertThat(ctx.diagnostics()).extracting(d -> d.message())
					.anyMatch(m -> m.contains("missing"));
		}

		@Test
		void addSupport_with_unknown_element_in_known_model_emits_error() {
			unit.add(new Justification("j"));
			AddSupport cmd = new AddSupport("j", "c", "s");
			ExecutionFailureDiagnostics.diagnoseDeadlock(List.of(cmd), unit,
					ctx);
			assertThat(ctx.hasErrors()).isTrue();
		}

		@Test
		void implementsTemplate_with_missing_model_emits_error() {
			ImplementsTemplate cmd = new ImplementsTemplate("missing", "base");
			ExecutionFailureDiagnostics.diagnoseDeadlock(List.of(cmd), unit,
					ctx);
			assertThat(ctx.diagnostics()).extracting(d -> d.message())
					.anyMatch(m -> m.contains("missing"));
		}

		@Test
		void implementsTemplate_with_missing_template_emits_error() {
			unit.add(new Justification("j"));
			ImplementsTemplate cmd = new ImplementsTemplate("j",
					"missingTemplate");
			ExecutionFailureDiagnostics.diagnoseDeadlock(List.of(cmd), unit,
					ctx);
			assertThat(ctx.hasErrors()).isTrue();
		}

		@Test
		void overrideAbstractSupport_with_unknown_model_emits_error() {
			OverrideAbstractSupport cmd = new OverrideAbstractSupport("missing",
					"id", "evidence", "lbl");
			ExecutionFailureDiagnostics.diagnoseDeadlock(List.of(cmd), unit,
					ctx);
			assertThat(ctx.diagnostics()).extracting(d -> d.message())
					.anyMatch(m -> m.contains("missing"));
		}

		@Test
		void unknown_command_type_emits_generic_error() {
			Command unknown = new CreateJustification("j");
			ExecutionFailureDiagnostics.diagnoseDeadlock(List.of(unknown), unit,
					ctx);
			assertThat(ctx.hasErrors()).isTrue();
		}

		@Test
		void always_appends_unresolved_symbol_summary() {
			AddSupport cmd = new AddSupport("missing", "c", "s");
			ExecutionFailureDiagnostics.diagnoseDeadlock(List.of(cmd), unit,
					ctx);
			assertThat(ctx.diagnostics()).extracting(d -> d.message())
					.anyMatch(m -> m.contains("unresolved symbol"));
		}
	}

	// -------------------------------------------------------------------------
	// diagnoseExecutionFailure
	// -------------------------------------------------------------------------

	@Nested
	class DiagnoseExecutionFailure {

		@Test
		void addSupport_emits_invalid_support_error() {
			AddSupport cmd = new AddSupport("j", "c", "s");
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(cmd, unit,
					new RuntimeException("bad support"), ctx);
			assertThat(ctx.diagnostics()).extracting(d -> d.message()).anyMatch(
					m -> m.contains("bad support") || m.contains("invalid"));
		}

		@Test
		void implementsTemplate_without_parent_emits_implements_error() {
			unit.add(new Justification("j"));
			ImplementsTemplate cmd = new ImplementsTemplate("j", "base");
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(cmd, unit,
					new RuntimeException("cycle"), ctx);
			assertThat(ctx.hasErrors()).isTrue();
		}

		@Test
		void implementsTemplate_with_parent_emits_cyclic_error() {
			Template t = new Template("base");
			unit.add(t);
			Justification j = new Justification("j");
			j.inline(t, "base");
			unit.add(j);
			ImplementsTemplate cmd = new ImplementsTemplate("j", "base");
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(cmd, unit,
					new RuntimeException("cycle"), ctx);
			assertThat(ctx.diagnostics()).extracting(d -> d.message())
					.anyMatch(m -> m.contains("cycle") || m.contains("cyclic"));
		}

		@Test
		void unknown_command_type_emits_execution_error() {
			Command unknown = new CreateJustification("j");
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(unknown, unit,
					new RuntimeException("oops"), ctx);
			assertThat(ctx.hasErrors()).isTrue();
		}

		@Test
		void always_appends_model_construction_failed_summary() {
			AddSupport cmd = new AddSupport("j", "c", "s");
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(cmd, unit,
					new RuntimeException("err"), ctx);
			assertThat(ctx.diagnostics()).extracting(d -> d.message())
					.anyMatch(m -> m.contains("model construction failed"));
		}
	}
}
