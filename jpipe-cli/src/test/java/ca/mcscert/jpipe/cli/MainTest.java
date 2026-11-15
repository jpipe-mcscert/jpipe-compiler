package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import picocli.CommandLine;

/** Unit tests for {@link Main}: default subcommand and global options. */
class MainTest {

	private static final CommandLine CMD = new CommandLine(new Main());

	// ---------------------------------------------------------------------------
	// Cases where no insertion should happen
	// ---------------------------------------------------------------------------

	static Stream<Arguments> subcommandAlreadyPresent() {
		return Stream.of(Arguments.of((Object) new String[]{"doctor"}),
				Arguments.of((Object) new String[]{"diagnostic"}),
				Arguments.of((Object) new String[]{"process"}),
				Arguments.of((Object) new String[]{"process", "-i", "foo.jd"}),
				Arguments.of((Object) new String[]{"doctor", "--help"}),
				Arguments.of((Object) new String[]{"--headless", "doctor"}));
	}

	@ParameterizedTest(name = "unchanged when subcommand present: {0}")
	@MethodSource("subcommandAlreadyPresent")
	void withDefaultSubcommand_returnsUnchanged_whenSubcommandPresent(
			String[] args) {
		assertThat(Main.withDefaultSubcommand(args, CMD)).isSameAs(args);
	}

	static Stream<Arguments> helpOrVersionFlags() {
		return Stream.of(Arguments.of((Object) new String[]{"--help"}),
				Arguments.of((Object) new String[]{"-h"}),
				Arguments.of((Object) new String[]{"--version"}),
				Arguments.of((Object) new String[]{"-V"}));
	}

	@ParameterizedTest(name = "unchanged for help/version flag: {0}")
	@MethodSource("helpOrVersionFlags")
	void withDefaultSubcommand_returnsUnchanged_whenHelpOrVersionFlag(
			String[] args) {
		assertThat(Main.withDefaultSubcommand(args, CMD)).isSameAs(args);
	}

	// ---------------------------------------------------------------------------
	// Cases where "process" should be injected
	// ---------------------------------------------------------------------------

	@Test
	void withDefaultSubcommand_insertsProcess_whenArgsEmpty() {
		assertThat(Main.withDefaultSubcommand(new String[]{}, CMD))
				.containsExactly("process");
	}

	@Test
	void withDefaultSubcommand_insertsProcessAtFront_whenNoParentFlags() {
		assertThat(Main.withDefaultSubcommand(
				new String[]{"-i", "foo.jd", "-d", "myDiagram"}, CMD))
				.containsExactly("process", "-i", "foo.jd", "-d", "myDiagram");
	}

	@Test
	void withDefaultSubcommand_insertsProcessAfterHeadless() {
		assertThat(Main.withDefaultSubcommand(
				new String[]{"--headless", "-i", "foo.jd"}, CMD))
				.containsExactly("--headless", "process", "-i", "foo.jd");
	}

	@Test
	void withDefaultSubcommand_insertsProcessAfterLogLevel() {
		assertThat(Main.withDefaultSubcommand(
				new String[]{"--log-level", "DEBUG", "-i", "foo.jd"}, CMD))
				.containsExactly("--log-level", "DEBUG", "process", "-i",
						"foo.jd");
	}

	@Test
	void withDefaultSubcommand_insertsProcessAfterAllParentFlags() {
		assertThat(Main.withDefaultSubcommand(new String[]{"--headless",
				"--log-level", "DEBUG", "-i", "foo.jd"}, CMD))
				.containsExactly("--headless", "--log-level", "DEBUG",
						"process", "-i", "foo.jd");
	}
	@Test
	void withDefaultSubcommand_acceptsTheEqualsFormOfAParentFlag() {
		// `--help` shows `--log-level=<level>`; that form used to be pushed
		// behind `process` and rejected as an unknown option (#186).
		assertThat(Main.withDefaultSubcommand(
				new String[]{"--log-level=INFO", "-i", "foo.jd"}, CMD))
				.containsExactly("--log-level=INFO", "process", "-i", "foo.jd");
	}

	@ParameterizedTest(name = "a later argument named like a subcommand: {0}")
	@ValueSource(strings = {"doctor", "diagnostic", "process", "--help", "-V"})
	void withDefaultSubcommand_onlyTheFirstTokenAfterParentFlagsCounts(
			String name) {
		// A model called `doctor` used to disable the default subcommand
		// (#186).
		assertThat(Main.withDefaultSubcommand(
				new String[]{"-i", "doc.jd", "-m", name}, CMD))
				.containsExactly("process", "-i", "doc.jd", "-m", name);
	}

	@Test
	void withDefaultSubcommand_leavesAParentFlagMissingItsValueToPicocli() {
		String[] args = {"--log-level"};
		assertThat(Main.withDefaultSubcommand(args, CMD)).isSameAs(args);
	}

	// ---------------------------------------------------------------------------
	// Options
	// ---------------------------------------------------------------------------

	@Test
	void anUnknownLogLevelIsRejected() {
		CommandLine cmd = Main.commandLine();
		StringWriter err = new StringWriter();
		cmd.setErr(new PrintWriter(err));

		int exitCode = cmd.execute("--log-level", "LOUD", "doctor");

		assertThat(exitCode).isEqualTo(CommandLine.ExitCode.USAGE);
		assertThat(err.toString()).contains("--log-level", "LOUD");
	}

	@ParameterizedTest(name = "{0} -V prints the version")
	@ValueSource(strings = {"process", "diagnostic", "doctor"})
	void everySubcommandPrintsTheVersion(String subcommand) {
		// The subcommands mixed in -V without a version provider, so it
		// printed nothing and exited 0 (#186).
		CommandLine cmd = Main.commandLine();
		StringWriter out = new StringWriter();
		cmd.setOut(new PrintWriter(out));

		int exitCode = cmd.execute(subcommand, "-V");

		assertThat(exitCode).isEqualTo(Main.EXIT_OK);
		assertThat(out.toString()).startsWith("jPipe ");
	}
}
