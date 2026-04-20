package ca.mcscert.jpipe.compiler.steps.checkers;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HaltAndCatchFireTest {

	private CompilationContext ctx;

	@BeforeEach
	void setUp() {
		ctx = new CompilationContext("file.jd");
	}

	@Test
	void context_with_no_errors_stays_non_fatal() {
		new HaltAndCatchFire<String>().fire("input", ctx);

		assertThat(ctx.hasFatalErrors()).isFalse();
	}

	@Test
	void context_with_errors_becomes_fatal() {
		ctx.error("some prior error");

		new HaltAndCatchFire<String>().fire("input", ctx);

		assertThat(ctx.hasFatalErrors()).isTrue();
	}

	@Test
	void input_is_returned_unchanged_when_no_errors() {
		String input = "unchanged";
		assertThat(new HaltAndCatchFire<String>().fire(input, ctx))
				.isSameAs(input);
	}
}
