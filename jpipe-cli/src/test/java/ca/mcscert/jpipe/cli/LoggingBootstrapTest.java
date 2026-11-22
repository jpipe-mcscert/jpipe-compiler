package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.cli.Main.LogLevel;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class LoggingBootstrapTest {

	/**
	 * Read where picocli reads it, among the parent flags; anything else falls
	 * back to the default, ERROR, and is left for picocli to judge.
	 */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"--headless process -i a.jd -m m              | ERROR",
			"--log-level INFO process -i a.jd             | INFO",
			"--log-level=debug diagnostic -i a.jd         | DEBUG",
			"--headless --log-level=TRACE doctor          | TRACE",
			"--log-level OFF process -i a.jd              | OFF",
			"--log-level=LOUD process                     | ERROR",
			"--log-level                                  | ERROR",
			"process --log-level INFO                     | ERROR"})
	void readsTheLevelAmongTheParentFlags(String commandLine,
			LogLevel expected) {
		assertThat(LoggingBootstrap.requestedLevel(commandLine.split(" ")))
				.isEqualTo(expected);
	}

	@ParameterizedTest
	@CsvSource({"OFF", "ERROR"})
	void aQuietRunUsesTheSimpleLoggerAtItsLevel(String level) {
		Properties system = new Properties();

		LoggingBootstrap.select(new String[]{"--log-level", level, "process"},
				system);

		assertThat(system)
				.containsEntry(LoggingBootstrap.PROVIDER,
						LoggingBootstrap.SIMPLE_PROVIDER)
				.containsEntry("org.apache.logging.log4j.simplelog.level",
						level);
		assertThat(LoggingBootstrap.usesSimpleLogger(system)).isTrue();
	}

	@Test
	void aDetailedRunKeepsLog4jCore() {
		Properties system = new Properties();

		LoggingBootstrap.select(new String[]{"--log-level=WARN", "process"},
				system);

		assertThat(system).isEmpty();
		assertThat(LoggingBootstrap.usesSimpleLogger(system)).isFalse();
	}

	@Test
	void aBackEndTheUserChoseIsKept() {
		Properties system = new Properties();
		system.setProperty(LoggingBootstrap.PROVIDER, "my.Provider");

		LoggingBootstrap.select(new String[]{"process"}, system);

		assertThat(system).hasSize(1).containsEntry(LoggingBootstrap.PROVIDER,
				"my.Provider");
	}
}
