package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.Command;

class UnexpectedErrorHandlerTest {

	/** A subcommand failing the way a compiler bug would. */
	@Command(name = "boom")
	static class Boom implements Callable<Integer> {
		@Override
		public Integer call() {
			throw new IllegalStateException("something broke");
		}
	}

	@Test
	void an_unexpected_exception_exits_42_with_one_line_and_no_stack_trace() {
		CommandLine cmd = Main.commandLine();
		cmd.addSubcommand(new Boom());
		StringWriter err = new StringWriter();
		cmd.setErr(new PrintWriter(err));

		int exitCode = cmd.execute("boom");

		assertThat(exitCode).isEqualTo(Main.EXIT_SYSTEM_ERROR);
		assertThat(err.toString()).startsWith(
				"internal error: java.lang.IllegalStateException: something broke")
				.contains(UnexpectedErrorHandler.ISSUES)
				.doesNotContain("\tat ");
	}
}
