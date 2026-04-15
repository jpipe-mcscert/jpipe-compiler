package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link Main#withDefaultSubcommand(String[])}. */
class MainTest {

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
		assertThat(Main.withDefaultSubcommand(args)).isSameAs(args);
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
		assertThat(Main.withDefaultSubcommand(args)).isSameAs(args);
	}

	// ---------------------------------------------------------------------------
	// Cases where "process" should be injected
	// ---------------------------------------------------------------------------

	@Test
	void withDefaultSubcommand_insertsProcess_whenArgsEmpty() {
		assertThat(Main.withDefaultSubcommand(new String[]{}))
				.containsExactly("process");
	}

	@Test
	void withDefaultSubcommand_insertsProcessAtFront_whenNoParentFlags() {
		assertThat(Main.withDefaultSubcommand(
				new String[]{"-i", "foo.jd", "-d", "myDiagram"}))
				.containsExactly("process", "-i", "foo.jd", "-d", "myDiagram");
	}

	@Test
	void withDefaultSubcommand_insertsProcessAfterHeadless() {
		assertThat(Main.withDefaultSubcommand(
				new String[]{"--headless", "-i", "foo.jd"}))
				.containsExactly("--headless", "process", "-i", "foo.jd");
	}

	@Test
	void withDefaultSubcommand_insertsProcessAfterLogLevel() {
		assertThat(Main.withDefaultSubcommand(
				new String[]{"--log-level", "DEBUG", "-i", "foo.jd"}))
				.containsExactly("--log-level", "DEBUG", "process", "-i",
						"foo.jd");
	}

	@Test
	void withDefaultSubcommand_insertsProcessAfterAllParentFlags() {
		assertThat(Main.withDefaultSubcommand(new String[]{"--headless",
				"--log-level", "DEBUG", "-i", "foo.jd"}))
				.containsExactly("--headless", "--log-level", "DEBUG",
						"process", "-i", "foo.jd");
	}
}
