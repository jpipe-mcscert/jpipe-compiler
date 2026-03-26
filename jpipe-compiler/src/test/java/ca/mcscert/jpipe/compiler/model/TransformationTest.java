package ca.mcscert.jpipe.compiler.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TransformationTest {

	private CompilationContext ctx;

	@BeforeEach
	void setUp() {
		ctx = new CompilationContext("file.jd");
	}

	// ── happy path ────────────────────────────────────────────────────────────

	@Test
	void fire_returnsResultOfRun() {
		Transformation<String, Integer> t = Transformation.of((input, c) -> 42);
		assertThat(t.fire("anything", ctx)).isEqualTo(42);
	}

	// ── null guard ────────────────────────────────────────────────────────────

	@Test
	void fire_throwsWhenRunReturnsNull() {
		Transformation<String, String> t = Transformation.of((input, c) -> null);
		assertThatThrownBy(() -> t.fire("input", ctx)).isInstanceOf(NullPointerException.class);
	}

	// ── exception handling ────────────────────────────────────────────────────

	@Test
	void fire_wrapsCheckedExceptionInCompilationException() {
		Transformation<String, String> t = Transformation.of((input, c) -> {
			throw new Exception("checked");
		});
		assertThatThrownBy(() -> t.fire("input", ctx)).isInstanceOf(CompilationException.class)
				.hasCauseInstanceOf(Exception.class).hasMessageContaining("checked");
	}

	@Test
	void fire_propagatesRuntimeExceptionUnwrapped() {
		RuntimeException original = new IllegalStateException("runtime");
		Transformation<String, String> t = Transformation.of((input, c) -> {
			throw original;
		});
		assertThatThrownBy(() -> t.fire("input", ctx)).isSameAs(original);
	}

	@Test
	void fire_propagatesCompilationExceptionUnwrapped() {
		CompilationException original = new CompilationException("step", "reason");
		Transformation<String, String> t = Transformation.of((input, c) -> {
			throw original;
		});
		assertThatThrownBy(() -> t.fire("input", ctx)).isSameAs(original);
	}

	// ── fast-fail ─────────────────────────────────────────────────────────────

	@Test
	void fire_fastFailsWhenContextHasFatalErrors() {
		ctx.fatal("previous step exploded");
		Transformation<String, String> t = Transformation.of((input, c) -> "should not run");
		assertThatThrownBy(() -> t.fire("input", ctx)).isInstanceOf(CompilationException.class)
				.hasMessageContaining("fatal");
	}

	@Test
	void fire_doesNotFastFailOnNonFatalErrors() {
		ctx.error("non-fatal");
		Transformation<String, Integer> t = Transformation.of((input, c) -> 7);
		assertThat(t.fire("input", ctx)).isEqualTo(7);
	}

	// ── composition ───────────────────────────────────────────────────────────

	@Test
	void andThen_composesResultsInOrder() {
		Transformation<String, Integer> length = Transformation.of((input, c) -> input.length());
		Transformation<Integer, String> label = Transformation.of((n, c) -> "len=" + n);
		assertThat(length.andThen(label).fire("hello", ctx)).isEqualTo("len=5");
	}

	@Test
	void andThen_threadsSameContextThroughBothSteps() {
		Transformation<String, String> reportError = Transformation.of((input, c) -> {
			c.error("from first step");
			return input;
		});
		Transformation<String, Integer> countErrors = Transformation
				.of((input, c) -> (int) c.diagnostics().stream().filter(Diagnostic::isError).count());

		int errorCount = reportError.andThen(countErrors).fire("x", ctx);
		assertThat(errorCount).isEqualTo(1);
	}

}
