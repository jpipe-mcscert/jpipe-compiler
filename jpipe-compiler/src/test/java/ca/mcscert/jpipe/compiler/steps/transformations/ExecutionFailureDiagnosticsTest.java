package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.CommandExecutionException;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.operators.ApplyOperator;
import ca.mcscert.jpipe.operators.ModelKind;
import ca.mcscert.jpipe.operators.OperatorCallConfig;
import ca.mcscert.jpipe.operators.OperatorRegistry;
import ca.mcscert.jpipe.operators.UnificationEquivalenceRegistry;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(cmd,
					new RuntimeException("bad support"), ctx);
			assertThat(ctx.diagnostics()).extracting(d -> d.message()).anyMatch(
					m -> m.contains("bad support") || m.contains("invalid"));
		}

		@Test
		void implementsTemplate_without_parent_emits_implements_error() {
			unit.add(new Justification("j"));
			ImplementsTemplate cmd = new ImplementsTemplate("j", "base");
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(cmd,
					new RuntimeException("cycle"), ctx);
			assertThat(ctx.hasErrors()).isTrue();
		}

		@Test
		void a_cycle_of_implements_is_reported_once_as_cyclic() {
			// A cycle never runs: each implements waits on the other.
			unit.add(new Template("t1"));
			unit.add(new Template("t2"));
			ExecutionFailureDiagnostics
					.diagnoseDeadlock(
							List.of(new ImplementsTemplate("t1", "t2"),
									new ImplementsTemplate("t2", "t1")),
							unit, ctx);
			assertThat(ctx.diagnostics()).filteredOn(d -> d.code() != null)
					.singleElement().satisfies(d -> {
						assertThat(d.code()).isEqualTo("cyclic-implements");
						assertThat(d.message()).isEqualTo(
								"cycle detected: t1 implements t2 implements t1");
					});
		}

		@Test
		void a_cycle_through_an_operator_call_is_reported_once_as_cyclic() {
			// t is built from j, which implements t: each waits on the other.
			// It used to be reported as an unknown model 't'.
			unit.add(new Justification("j"));
			unit.add(new Justification("a"));
			ExecutionFailureDiagnostics.diagnoseDeadlock(
					List.of(new ImplementsTemplate("j", "t"),
							call("t", "assemble", "j", "a")),
					unit, Set.of(), ctx);
			assertThat(ctx.diagnostics()).filteredOn(d -> d.code() != null)
					.singleElement().satisfies(d -> {
						assertThat(d.code()).isEqualTo("cyclic-implements");
						assertThat(d.message())
								.isEqualTo("cycle detected: j implements t,"
										+ " which 'assemble' builds from j");
					});
		}

		@Test
		void implementing_a_template_whose_call_failed_adds_no_error() {
			// The call's failure is the one cause: 't' is declared, and was
			// reported as an unknown model (#183).
			unit.add(new Justification("j"));
			ExecutionFailureDiagnostics.diagnoseDeadlock(
					List.of(new ImplementsTemplate("j", "t")), unit,
					Set.of("t"), ctx);
			assertThat(ctx.diagnostics()).isEmpty();
		}

		@Test
		void implementing_a_template_never_built_adds_no_error() {
			// The call that builds 't' reports its unknown source.
			unit.add(new Justification("j"));
			unit.add(new Justification("a"));
			ExecutionFailureDiagnostics.diagnoseDeadlock(
					List.of(new ImplementsTemplate("j", "t"),
							call("t", "refine", "typo", "a")),
					unit, Set.of(), ctx);
			assertThat(ctx.diagnostics()).filteredOn(d -> d.code() != null)
					.singleElement().satisfies(d -> assertThat(d.message())
							.contains("'typo'", "'refine' for 't'"));
		}

		@Test
		void unknown_command_type_emits_execution_error() {
			Command unknown = new CreateJustification("j");
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(unknown,
					new RuntimeException("oops"), ctx);
			assertThat(ctx.hasErrors()).isTrue();
		}

		@Test
		void a_command_with_a_location_is_reported_there() {
			// Duplicate models, conclusions and the like used to be reported
			// without a line (#183).
			Command cmd = new CreateJustification("j", AT_3_4);
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(cmd,
					new IllegalStateException("Duplicate model name: 'j'"),
					ctx);
			assertThat(ctx.diagnostics()).singleElement().satisfies(d -> {
				assertThat(d.line()).isEqualTo(3);
				assertThat(d.column()).isEqualTo(4);
				assertThat(d.message()).isEqualTo("Duplicate model name: 'j'");
			});
		}

		@Test
		void an_operator_call_failure_names_the_operator_at_the_call() {
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(
					call("r", "assemble", "a", "b"),
					new IllegalArgumentException("missing required argument(s)"
							+ " [strategyLabel]"),
					ctx);
			assertThat(ctx.diagnostics()).singleElement().satisfies(d -> {
				assertThat(d.line()).isEqualTo(3);
				assertThat(d.message()).isEqualTo(
						"'assemble' for 'r': missing required argument(s)"
								+ " [strategyLabel]");
			});
		}

		@Test
		void several_failures_are_reported_with_one_summary() {
			Unit partial = unit;
			List<CommandExecutionException> failures = List.of(
					new CommandExecutionException(new AddSupport("j", "c", "s"),
							partial, new RuntimeException("first")),
					new CommandExecutionException(new AddSupport("j", "c", "t"),
							partial, new RuntimeException("second")));
			ExecutionFailureDiagnostics.diagnoseExecutionFailures(failures,
					ctx);
			assertThat(ctx.diagnostics()).extracting(d -> d.message())
					.contains("first", "second")
					.filteredOn(m -> m.contains("model construction failed"))
					.hasSize(1);
		}
	}

	@Nested
	class DiagnoseStuckOperatorsAndOverrides {

		@Test
		void a_call_waiting_on_a_model_whose_own_call_failed_is_not_reported() {
			// The failure of 'z' is reported once; the call that uses 'z' is
			// no separate mistake (#183).
			unit.add(new Justification("a"));
			ApplyOperator failed = call("z", "frobnicate", "a");
			List<CommandExecutionException> failures = List
					.of(new CommandExecutionException(failed, unit,
							new IllegalArgumentException("unknown operator")));
			ExecutionFailureDiagnostics.diagnoseDeadlock(
					List.of(call("w", "refine", "z", "a")), unit,
					ExecutionFailureDiagnostics.brokenModels(failures), ctx);
			assertThat(ctx.diagnostics()).isEmpty();
		}

		@Test
		void what_waits_on_a_failed_call_is_not_reported_however_deep() {
			// x waits on w, which waits on z, whose call failed: z's failure
			// is the one cause, reported once.
			unit.add(new Justification("a"));
			Set<String> failed = Set.of("z");
			ExecutionFailureDiagnostics
					.diagnoseDeadlock(
							List.of(call("w", "refine", "z", "a"),
									call("x", "refine", "w", "a")),
							unit, failed, ctx);
			assertThat(ctx.diagnostics()).isEmpty();
		}

		@Test
		void a_broken_source_does_not_hide_an_unknown_one() {
			unit.add(new Justification("a"));
			ExecutionFailureDiagnostics.diagnoseDeadlock(
					List.of(call("v", "refine", "z", "typo")), unit,
					Set.of("z"), ctx);
			assertThat(ctx.diagnostics()).first().satisfies(d -> {
				assertThat(d.code()).isEqualTo("unknown-model");
				assertThat(d.message()).contains("'typo'")
						.doesNotContain("'z'");
			});
		}

		@Test
		void calls_that_only_wait_on_each_other_are_reported() {
			// Nothing else explains a cycle: it must not vanish.
			unit.add(new Justification("c"));
			ExecutionFailureDiagnostics.diagnoseDeadlock(
					List.of(call("a", "refine", "b", "c"),
							call("b", "refine", "a", "c")),
					unit, Set.of(), ctx);
			assertThat(ctx.diagnostics()).filteredOn(d -> d.code() != null)
					.extracting(d -> d.message()).containsExactly(
							"cannot apply 'refine' for 'a': it waits on 'b',"
									+ " which is never built",
							"cannot apply 'refine' for 'b': it waits on 'a',"
									+ " which is never built");
		}

		@Test
		void a_failure_without_a_message_is_named_by_its_type() {
			ExecutionFailureDiagnostics.diagnoseExecutionFailure(
					new CreateJustification("j", AT_3_4),
					new NullPointerException(), ctx);
			assertThat(ctx.diagnostics()).singleElement()
					.satisfies(d -> assertThat(d.message())
							.isEqualTo("NullPointerException"));
		}

		@Test
		void an_operator_call_waiting_for_an_unknown_model_names_it() {
			// It used to read `cannot execute: applyOperator(...)`, with no
			// line and without naming the missing model (#183).
			unit.add(new Justification("a"));
			ExecutionFailureDiagnostics.diagnoseDeadlock(
					List.of(call("r", "refine", "a", "typo")), unit, ctx);
			assertThat(ctx.diagnostics()).first().satisfies(d -> {
				assertThat(d.code()).isEqualTo("unknown-model");
				assertThat(d.line()).isEqualTo(3);
				assertThat(d.message()).contains("'typo'", "'refine' for 'r'");
			});
		}

		@Test
		void overriding_a_concrete_element_says_so() {
			Justification j = new Justification("j");
			j.addElement(new Evidence("t:abs", "already overridden"));
			unit.add(j);
			OverrideAbstractSupport cmd = new OverrideAbstractSupport("j",
					"t:abs", "evidence", "again", AT_3_4);
			ExecutionFailureDiagnostics.diagnoseDeadlock(List.of(cmd), unit,
					ctx);
			assertThat(ctx.diagnostics()).first().satisfies(d -> {
				assertThat(d.code()).isEqualTo("unresolved-override");
				assertThat(d.message()).contains("not an abstract support");
			});
		}
	}

	private static final SourceLocation AT_3_4 = new SourceLocation(3, 4);

	private static ApplyOperator call(String result, String operator,
			String... sources) {
		return new ApplyOperator(
				new OperatorCallConfig(result, operator, List.of(sources),
						Map.of(), AT_3_4, ModelKind.JUSTIFICATION),
				new OperatorRegistry(), new UnificationEquivalenceRegistry());
	}
}
