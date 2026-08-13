package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DoctorTest {

	/** A name no executable on PATH is expected to have. */
	private static final String MISSING_TOOL = "jpipe-no-such-tool-42";

	/** The executable of the JVM running the tests: always available. */
	private static String javaBinary() {
		return System.getProperty("java.home") + File.separator + "bin"
				+ File.separator + "java";
	}

	/** Runs the doctor on the given tools, capturing what it reports. */
	private static String capturedRun(Map<String, String[]> tools) {
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		try (PrintStream out = new PrintStream(captured, true,
				StandardCharsets.UTF_8)) {
			Doctor.run(tools, out);
		}
		return captured.toString(StandardCharsets.UTF_8);
	}

	/** Runs the doctor on the given tools, discarding what it reports. */
	private static boolean silentRun(Map<String, String[]> tools) {
		try (PrintStream out = new PrintStream(OutputStream.nullOutputStream(),
				true, StandardCharsets.UTF_8)) {
			return Doctor.run(tools, out);
		}
	}

	@Test
	void run_returns_boolean() {
		boolean result;
		try (PrintStream out = new PrintStream(OutputStream.nullOutputStream(),
				true, StandardCharsets.UTF_8)) {
			result = Doctor.run(out);
		}
		assertThat(result).isIn(true, false);
	}

	@Test
	void run_reports_a_status_line_per_tool() {
		String output = capturedRun(
				Map.of("dot (Graphviz)", new String[]{"dot", "-V"}));
		assertThat(output).startsWith("  dot (Graphviz): ").containsPattern(
				"dot \\(Graphviz\\): (OK \\(version .+\\)|NOT FOUND)");
	}

	// The runtime tools are not guaranteed to be installed where the tests run,
	// so the probe is exercised against the running JVM, which always is.

	@Test
	void run_reports_every_tool_and_fails_on_a_missing_one() {
		Map<String, String[]> tools = new LinkedHashMap<>();
		tools.put("java (JVM)", new String[]{javaBinary(), "-version"});
		tools.put("ghost", new String[]{MISSING_TOOL});
		String output = capturedRun(tools);
		assertThat(output).contains("  java (JVM): OK (version ")
				.contains("  ghost: NOT FOUND");
	}

	@Test
	void run_succeeds_when_every_tool_is_available() {
		boolean allOk = silentRun(
				Map.of("java (JVM)", new String[]{javaBinary(), "-version"}));
		assertThat(allOk).isTrue();
	}

	@Test
	void run_fails_when_a_tool_is_missing() {
		boolean allOk = silentRun(Map.of("ghost", new String[]{MISSING_TOOL}));
		assertThat(allOk).isFalse();
	}

	@Test
	void probe_captures_the_output_of_an_existing_tool() {
		Optional<String> banner = Doctor
				.probe(new String[]{javaBinary(), "-version"});
		assertThat(banner).hasValueSatisfying(
				b -> assertThat(b).containsIgnoringCase("version"));
	}

	@Test
	void probe_is_empty_when_the_tool_cannot_be_launched() {
		assertThat(Doctor.probe(new String[]{MISSING_TOOL})).isEmpty();
	}

	@Test
	void statusLine_reports_the_version_of_an_available_tool() {
		assertThat(Doctor.statusLine("dot (Graphviz)",
				Optional.of("dot - graphviz version 12.2.1 (20241206.2353)")))
				.isEqualTo("  dot (Graphviz): OK (version 12.2.1)");
	}

	@Test
	void statusLine_reports_a_missing_tool() {
		assertThat(Doctor.statusLine("dot (Graphviz)", Optional.empty()))
				.isEqualTo("  dot (Graphviz): NOT FOUND");
	}

	@ParameterizedTest(name = "extracts {1} out of \"{0}\"")
	@CsvSource({"dot - graphviz version 12.2.1 (20241206.2353), version 12.2.1",
			"dot - graphviz version 2.43.0 (0), version 2.43.0",
			"Graphviz VERSION v1.2.3-beta, version 1.2.3-beta"})
	void describe_extracts_the_advertised_version(String banner,
			String expected) {
		assertThat(Doctor.describe(banner)).isEqualTo(expected);
	}

	@ParameterizedTest(name = "falls back to unknown for \"{0}\"")
	@ValueSource(strings = {"", "some tool with no version banner",
			"version without a number"})
	void describe_falls_back_when_no_version_is_advertised(String banner) {
		assertThat(Doctor.describe(banner)).isEqualTo("version unknown");
	}
}
