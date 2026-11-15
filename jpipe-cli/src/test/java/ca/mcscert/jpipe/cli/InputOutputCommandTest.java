package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.function.IntSupplier;
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
	void call_missing_file_is_reported_by_diagnostic() {
		// `diagnostic` reports on a source rather than consuming it, so a file
		// it cannot read is a fatal diagnostic like any other, not a crash.
		int result = Main.commandLine().execute("--headless", "diagnostic",
				"-i", "/no/such/file.jd", "-o", "<stdout>");
		assertThat(result).isEqualTo(Main.EXIT_JPIPE_ERROR);
	}

	@Test
	void call_missing_file_returns_system_error_when_processing() {
		int result = Main.commandLine().execute("--headless", "process", "-i",
				"/no/such/file.jd", "-m", "whatever", "-o", "<stdout>");
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
	void the_logo_goes_to_stderr_and_never_to_stdout() {
		String input = resourcePath("test_minimal.jd");
		Captured run = capture(() -> Main.commandLine().execute("diagnostic",
				"-i", input, "-o", "<stdout>"));

		assertThat(run.err()).contains("McMaster");
		assertThat(run.out()).doesNotContain("McMaster")
				.startsWith("=== Diagnostics ===");
	}

	@Test
	void process_json_to_stdout_emits_a_parseable_document_without_headless() {
		// Every export written to stdout used to start with the logo (#175).
		String input = resourcePath("test_minimal.jd");
		Captured run = capture(() -> Main.commandLine().execute("process", "-i",
				input, "-m", "minimal", "-f", "json"));

		assertThat(run.exitCode()).isEqualTo(Main.EXIT_OK);
		assertThat(new JSONObject(run.out()).getString("name"))
				.isEqualTo("minimal");
	}

	/** What one invocation wrote to each stream, and how it exited. */
	private record Captured(int exitCode, String out, String err) {
	}

	/** Runs {@code command} with both standard streams captured. */
	private static Captured capture(IntSupplier command) {
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		int exitCode;
		try (PrintStream outStream = new PrintStream(out, true,
				StandardCharsets.UTF_8);
				PrintStream errStream = new PrintStream(err, true,
						StandardCharsets.UTF_8)) {
			System.setOut(outStream);
			System.setErr(errStream);
			exitCode = command.getAsInt();
		} finally {
			System.setOut(originalOut);
			System.setErr(originalErr);
		}
		return new Captured(exitCode, out.toString(StandardCharsets.UTF_8),
				err.toString(StandardCharsets.UTF_8));
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
