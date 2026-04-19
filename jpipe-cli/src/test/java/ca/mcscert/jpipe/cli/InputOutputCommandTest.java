package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URL;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

class InputOutputCommandTest {

	@Test
	void call_valid_file_returns_exit_ok() {
		String input = resourcePath("test_minimal.jd");
		int result = new CommandLine(new Main()).execute("--headless",
				"diagnostic", "-i", input, "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_OK);
	}

	@Test
	void call_invalid_file_returns_jpipe_error() {
		String input = resourcePath("test_invalid.jd");
		int result = new CommandLine(new Main()).execute("--headless",
				"diagnostic", "-i", input, "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_JPIPE_ERROR);
	}

	@Test
	void call_missing_file_returns_system_error() {
		int result = new CommandLine(new Main()).execute("--headless",
				"diagnostic", "-i", "/no/such/file.jd", "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_SYSTEM_ERROR);
	}

	@Test
	void process_valid_file_returns_exit_ok() {
		String input = resourcePath("test_minimal.jd");
		int result = new CommandLine(new Main()).execute("--headless",
				"process", "-i", input, "-m", "minimal", "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_OK);
	}

	@Test
	void process_unknown_model_returns_jpipe_error() {
		String input = resourcePath("test_minimal.jd");
		int result = new CommandLine(new Main()).execute("--headless",
				"process", "-i", input, "-m", "no_such_model", "-o",
				"<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_JPIPE_ERROR);
	}

	@Test
	void doctor_subcommand_returns_result() {
		int result = new CommandLine(new Main()).execute("--headless",
				"doctor");
		assertThat(result).isIn(Main.EXIT_OK, Main.EXIT_JPIPE_ERROR);
	}

	@Test
	void log_level_option_is_accepted() {
		String input = resourcePath("test_minimal.jd");
		int result = new CommandLine(new Main()).execute("--headless",
				"--log-level", "INFO", "diagnostic", "-i", input, "-o",
				"<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_OK);
	}

	private static String resourcePath(String name) {
		URL url = InputOutputCommandTest.class.getClassLoader()
				.getResource(name);
		if (url == null) {
			throw new IllegalStateException("Test resource not found: " + name);
		}
		return url.getPath();
	}
}
