package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.IntSupplier;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

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
	void doctor_prints_the_logo_on_stderr_too() {
		Captured run = capture(() -> Main.commandLine().execute("doctor"));

		assertThat(run.err()).contains("McMaster");
		assertThat(run.out()).doesNotContain("McMaster")
				.contains("Checking external tools:");
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

	@Test
	void an_output_that_is_the_input_is_refused_and_the_source_kept(
			@TempDir Path dir) throws IOException {
		// The output used to be truncated before the input was read, so the
		// source was erased and the error blamed its syntax (#174).
		Path source = dir.resolve("same.jd");
		Files.copy(Path.of(resourcePath("test_minimal.jd")), source);
		String before = Files.readString(source);

		Captured run = capture(() -> Main.commandLine().execute("--headless",
				"process", "-f", "jpipe", "-i", source.toString(), "-m",
				"minimal", "-o", source.toString()));

		assertThat(run.exitCode()).isEqualTo(CommandLine.ExitCode.USAGE);
		assertThat(run.err()).contains("is also the input");
		assertThat(source).hasContent(before);
	}

	@Test
	void a_failed_run_leaves_the_previous_output_as_it_was(@TempDir Path dir)
			throws IOException {
		Path previous = Files.writeString(dir.resolve("out.dot"), "precious");

		int exitCode = Main.commandLine().execute("--headless", "process", "-i",
				resourcePath("test_minimal.jd"), "-m", "nope", "-f", "dot",
				"-o", previous.toString());

		assertThat(exitCode).isEqualTo(Main.EXIT_JPIPE_ERROR);
		assertThat(previous).hasContent("precious");
	}

	@Test
	void a_run_with_non_fatal_errors_still_writes_its_export(@TempDir Path dir)
			throws IOException {
		// ADR-0016: a semantic error does not stop the export, so the file is
		// written and the run exits 1. Only an abort keeps the previous file.
		Path source = Files.writeString(dir.resolve("incomplete.jd"), """
				justification j {
				    conclusion c is "C"
				    strategy s is "S"
				    s supports c
				}
				""");
		Path out = Files.writeString(dir.resolve("out.json"), "previous");

		int exitCode = Main.commandLine().execute("--headless", "process", "-i",
				source.toString(), "-m", "j", "-f", "json", "-o",
				out.toString());

		assertThat(exitCode).isEqualTo(Main.EXIT_JPIPE_ERROR);
		assertThat(out).content().startsWith("{");
	}

	@Test
	void a_successful_run_writes_the_output_file(@TempDir Path dir) {
		Path out = dir.resolve("out.json");

		int exitCode = Main.commandLine().execute("--headless", "process", "-i",
				resourcePath("test_minimal.jd"), "-m", "minimal", "-f", "json",
				"-o", out.toString());

		assertThat(exitCode).isEqualTo(Main.EXIT_OK);
		assertThat(out).content().startsWith("{");
	}

	@Test
	void an_output_that_cannot_be_written_is_named_in_the_error(
			@TempDir Path dir) {
		// It used to read `unexpected error: … (Is a directory)` (#186).
		Captured run = capture(() -> Main.commandLine().execute("--headless",
				"process", "-i", resourcePath("test_minimal.jd"), "-m",
				"minimal", "-o", dir.toString()));

		assertThat(run.exitCode()).isEqualTo(Main.EXIT_SYSTEM_ERROR);
		assertThat(run.err())
				.contains("error: cannot write '" + dir + "': is a directory")
				.doesNotContain("unexpected");
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
