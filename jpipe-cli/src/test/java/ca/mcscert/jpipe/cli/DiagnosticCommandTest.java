package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.DiagnosticFormat;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

class DiagnosticCommandTest {

	@Test
	void doCall_valid_file_returns_exit_ok() throws Exception {
		DiagnosticCommand cmd = new DiagnosticCommand();
		cmd.input = resourcePath("test_minimal.jd");
		cmd.output = CompilationConfig.STDOUT;

		Integer result = cmd.doCall(new ByteArrayOutputStream());

		assertThat(result).isEqualTo(Main.EXIT_OK);
	}

	@Test
	void doCall_invalid_syntax_still_writes_a_report() throws Exception {
		// A file being edited is broken most of the time: that is exactly when
		// a tool needs a report rather than an empty stream (#154).
		DiagnosticCommand cmd = new DiagnosticCommand();
		cmd.input = resourcePath("test_invalid.jd");
		cmd.output = CompilationConfig.STDOUT;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		Integer result = cmd.doCall(out);

		assertThat(result).isEqualTo(Main.EXIT_JPIPE_ERROR);
		assertThat(out.toString(StandardCharsets.UTF_8))
				.contains("=== Diagnostics ===").contains("[FATAL]");
	}

	@Test
	void doCall_invalid_syntax_writes_a_parseable_json_report()
			throws Exception {
		DiagnosticCommand cmd = new DiagnosticCommand();
		cmd.input = resourcePath("test_invalid.jd");
		cmd.output = CompilationConfig.STDOUT;
		cmd.format = DiagnosticFormat.JSON;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		Integer result = cmd.doCall(out);

		JSONObject report = new JSONObject(
				out.toString(StandardCharsets.UTF_8));
		assertThat(result).isEqualTo(Main.EXIT_JPIPE_ERROR);
		assertThat(report.getString("status")).isEqualTo("errors");
		assertThat(report.getJSONArray("models")).isEmpty();
		assertThat(severities(report)).contains("fatal");
	}

	/** The severity of every diagnostic in a JSON report. */
	private static List<String> severities(JSONObject report) {
		return report.getJSONArray("diagnostics").toList().stream()
				.map(d -> (String) ((Map<?, ?>) d).get("severity")).toList();
	}

	@Test
	void doCall_defaults_to_the_text_report() throws Exception {
		DiagnosticCommand cmd = new DiagnosticCommand();
		cmd.input = resourcePath("test_minimal.jd");
		cmd.output = CompilationConfig.STDOUT;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		cmd.doCall(out);

		assertThat(out.toString(StandardCharsets.UTF_8))
				.contains("=== Diagnostics ===");
	}

	@Test
	void doCall_json_format_writes_a_json_document() throws Exception {
		DiagnosticCommand cmd = new DiagnosticCommand();
		cmd.input = resourcePath("test_minimal.jd");
		cmd.output = CompilationConfig.STDOUT;
		cmd.format = DiagnosticFormat.JSON;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		cmd.doCall(out);

		JSONObject report = new JSONObject(
				out.toString(StandardCharsets.UTF_8));
		assertThat(report.getString("status")).isEqualTo("ok");
		assertThat(report.getJSONArray("diagnostics")).isEmpty();
	}

	@Test
	void suppressLogo_onlyForJsonOnStdout() {
		DiagnosticCommand cmd = new DiagnosticCommand();
		cmd.output = CompilationConfig.STDOUT;

		cmd.format = DiagnosticFormat.TEXT;
		assertThat(cmd.suppressLogo()).isFalse();

		cmd.format = DiagnosticFormat.JSON;
		assertThat(cmd.suppressLogo()).isTrue();

		cmd.output = "report.json";
		assertThat(cmd.suppressLogo()).isFalse();
	}

	private static String resourcePath(String name) {
		URL url = DiagnosticCommandTest.class.getClassLoader()
				.getResource(name);
		if (url == null) {
			throw new IllegalStateException("Test resource not found: " + name);
		}
		return url.getPath();
	}
}
