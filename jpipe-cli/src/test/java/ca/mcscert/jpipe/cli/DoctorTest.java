package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DoctorTest {

	@Test
	void run_returns_boolean() {
		boolean result = Doctor.run();
		assertThat(result).isIn(true, false);
	}

	@Test
	void run_reports_a_status_line_per_tool() {
		PrintStream original = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		System.setOut(new PrintStream(captured, true, StandardCharsets.UTF_8));
		try {
			Doctor.run();
		} finally {
			System.setOut(original);
		}
		assertThat(captured.toString(StandardCharsets.UTF_8))
				.startsWith("  dot (Graphviz): ").containsPattern(
						"dot \\(Graphviz\\): (OK \\(version .+\\)|NOT FOUND)");
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
