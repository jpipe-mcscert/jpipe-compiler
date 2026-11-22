package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.cli.Main.LogLevel;
import java.util.Map;
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
				system, Map.of());

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
				system, Map.of());

		assertThat(system).isEmpty();
		assertThat(LoggingBootstrap.usesSimpleLogger(system)).isFalse();
	}

	/**
	 * Log4j starts as it always did when the user configured it, or when
	 * --log-level may hide in an @file that picocli expands only later.
	 */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
			"log4j.provider=my.Provider |  | process",
			"log4j2.configurationFile=mine.xml |  | process",
			"org.apache.logging.log4j.simplelog.level=TRACE |  | process",
			" | LOG4J_PROVIDER=my.Provider | process",
			" |  | @opts.txt process"})
	void log4jIsLeftAloneWhenTheBootstrapCannotBeSure(String property,
			String variable, String commandLine) {
		Properties system = new Properties();
		if (property != null) {
			String[] kv = property.split("=");
			system.setProperty(kv[0], kv[1]);
		}
		Map<String, String> env = variable == null
				? Map.of()
				: Map.of(variable.split("=")[0], variable.split("=")[1]);
		Properties before = (Properties) system.clone();

		LoggingBootstrap.select(commandLine.split(" "), system, env);

		assertThat(system).isEqualTo(before);
		assertThat(LoggingBootstrap.usesSimpleLogger(system)).isFalse();
	}
}
