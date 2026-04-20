package ca.mcscert.jpipe.compiler.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CheckerTest {

	private CompilationContext ctx;

	@BeforeEach
	void setUp() {
		ctx = new CompilationContext("file.jd");
	}

	@Test
	void fire_returnsInputUnchanged() {
		String input = "original";
		Checker<String> noOp = Checker.checking((value, c) -> {
		});
		assertThat(noOp.fire(input, ctx)).isSameAs(input);
	}

	@Test
	void check_canAccumulateNonFatalErrors() {
		Checker<String> lenCheck = Checker.checking((value, c) -> {
			if (value.isEmpty())
				c.error("value must not be empty");
		});
		lenCheck.fire("", ctx);
		assertThat(ctx.hasErrors()).isTrue();
		assertThat(ctx.hasFatalErrors()).isFalse();
	}

	@Test
	void check_throwingRuntimeExceptionPropagatesUnwrapped() {
		RuntimeException original = new IllegalArgumentException("bad input");
		Checker<String> failing = Checker.checking((value, c) -> {
			throw original;
		});
		assertThatThrownBy(() -> failing.fire("x", ctx)).isSameAs(original);
	}

	@Test
	void check_reportingFatalThenComposingFastFails() {
		Checker<String> markFatal = Checker
				.checking((value, c) -> c.fatal("unrecoverable"));
		Transformation<String, Integer> next = Transformation
				.of((input, c) -> input.length());

		var chain = markFatal.andThen(next);
		assertThatThrownBy(() -> chain.fire("hello", ctx))
				.isInstanceOf(CompilationException.class)
				.hasMessageContaining("fatal");
	}

}
