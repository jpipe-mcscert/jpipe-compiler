package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class InputOutputCommandTest {

	@Test
	void call_valid_file_returns_exit_ok() {
		String input = resourcePath("test_minimal.jd");
		int result = Main.commandLine().execute("--headless", "diagnostic",
				"-i", input, "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_OK);
	}

	@Test
	void call_invalid_file_returns_jpipe_error() {
		String input = resourcePath("test_invalid.jd");
		int result = Main.commandLine().execute("--headless", "diagnostic",
				"-i", input, "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_JPIPE_ERROR);
	}

	@Test
	void call_missing_file_returns_system_error() {
		int result = Main.commandLine().execute("--headless", "diagnostic",
				"-i", "/no/such/file.jd", "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_SYSTEM_ERROR);
	}

	@Test
	void process_valid_file_returns_exit_ok() {
		String input = resourcePath("test_minimal.jd");
		int result = Main.commandLine().execute("--headless", "process", "-i",
				input, "-m", "minimal", "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_OK);
	}

	@Test
	void process_unknown_model_returns_jpipe_error() {
		String input = resourcePath("test_minimal.jd");
		int result = Main.commandLine().execute("--headless", "process", "-i",
				input, "-m", "no_such_model", "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_JPIPE_ERROR);
	}

	@Test
	void doctor_subcommand_returns_result() {
		int result = Main.commandLine().execute("--headless", "doctor");
		assertThat(result).isIn(Main.EXIT_OK, Main.EXIT_JPIPE_ERROR);
	}

	@Test
	void log_level_option_is_accepted() {
		String input = resourcePath("test_minimal.jd");
		int result = Main.commandLine().execute("--headless", "--log-level",
				"INFO", "diagnostic", "-i", input, "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_OK);
	}

	@Test
	void diagnostic_json_format_is_accepted() {
		String input = resourcePath("test_minimal.jd");
		int result = Main.commandLine().execute("--headless", "diagnostic",
				"-i", input, "-f", "json", "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_OK);
	}

	@Test
	void diagnostic_json_format_is_case_insensitive() {
		String input = resourcePath("test_minimal.jd");
		int result = Main.commandLine().execute("--headless", "diagnostic",
				"-i", input, "-f", "JSON", "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_OK);
	}

	@Test
	void diagnostic_unknown_format_is_rejected() {
		String input = resourcePath("test_minimal.jd");
		int result = Main.commandLine().execute("--headless", "diagnostic",
				"-i", input, "-f", "yaml", "-o", "<stdout>");
		assertThat(result).isNotEqualTo(Main.EXIT_OK);
	}

	@Test
	void diagnostic_json_to_stdout_emits_a_parseable_document() {
		String input = resourcePath("test_minimal.jd");
		PrintStream original = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		int result;
		try (PrintStream redirected = new PrintStream(captured, true,
				StandardCharsets.UTF_8)) {
			System.setOut(redirected);
			// Deliberately NOT --headless: the logo must step aside on its own,
			// or every IDE integration would have to strip it.
			result = Main.commandLine().execute("diagnostic", "-i", input, "-f",
					"json", "-o", "<stdout>");
		} finally {
			System.setOut(original);
		}

		assertThat(result).isEqualTo(Main.EXIT_OK);
		String output = captured.toString(StandardCharsets.UTF_8);
		assertThat(output).doesNotContain("McMaster").startsWith("{");
		assertThat(new JSONObject(output).getInt("schemaVersion")).isEqualTo(1);
	}

	@Test
	void diagnostic_text_to_stdout_still_shows_the_logo() {
		String input = resourcePath("test_minimal.jd");
		PrintStream original = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try (PrintStream redirected = new PrintStream(captured, true,
				StandardCharsets.UTF_8)) {
			System.setOut(redirected);
			Main.commandLine().execute("diagnostic", "-i", input, "-o",
					"<stdout>");
		} finally {
			System.setOut(original);
		}

		assertThat(captured.toString(StandardCharsets.UTF_8))
				.contains("McMaster");
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
