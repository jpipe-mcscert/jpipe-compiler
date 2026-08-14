package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Smoke tests running the shaded JAR as a user would: {@code java -jar}.
 *
 * <p>
 * The unit and Cucumber suites run against each module's own classpath, so a
 * packaging mistake — a dependency the shade plugin dropped, a clobbered
 * {@code META-INF} service file, a resource left out of the JAR — passes them
 * all while the shipped CLI is broken. These tests exercise the artifact
 * itself, once per output path, asserting exit codes and output shape rather
 * than exact bytes.
 *
 * <p>
 * Named {@code *IT} so failsafe runs them after {@code package}, when the JAR
 * exists. They are part of {@code mvn verify}.
 */
class ShadedJarIT {

	private static final String MINIMAL = example("000_minimal.jd");

	/** Outcome of one CLI invocation. */
	private record Execution(int exitCode, String out, String err) {
	}

	// ── the JAR loads and each output path runs ──────────────────────────────

	@ParameterizedTest(name = "{0} exits 0 and writes {1}")
	@CsvSource({"diagnostic, === Diagnostics ===", "process, digraph"})
	void everyOutputPathRuns(String subcommand, String expected)
			throws IOException {
		Execution run = subcommand.equals("diagnostic")
				? jpipe("--headless", "diagnostic", "-i", MINIMAL)
				: jpipe("--headless", "process", "-i", MINIMAL, "-m", "minimal",
						"-f", "dot");

		assertThat(run.exitCode()).isZero();
		assertThat(run.out()).contains(expected);
	}

	@Test
	void theDiagnosticReportIsParseableJson() throws IOException {
		// org.json is a transitive dependency: it went missing from the JAR
		// once, and every unit test stayed green (#153).
		Execution run = jpipe("--headless", "diagnostic", "-i", MINIMAL, "-f",
				"json");

		assertThat(run.exitCode()).isZero();
		assertThat(new JSONObject(run.out()).getInt("schemaVersion"))
				.isEqualTo(1);
	}

	@Test
	void theExportedModelIsParseableJson() throws IOException {
		Execution run = jpipe("--headless", "process", "-i", MINIMAL, "-m",
				"minimal", "-f", "json");

		assertThat(run.exitCode()).isZero();
		assertThat(new JSONObject(run.out()).getString("name"))
				.isEqualTo("minimal");
	}

	// ── the contract tools rely on ───────────────────────────────────────────

	@Test
	void theJsonReportStaysParseableWithoutHeadless() throws IOException {
		// The logo must step aside on its own when the report goes to stdout,
		// or every IDE integration would have to strip it.
		Execution run = jpipe("diagnostic", "-i", MINIMAL, "-f", "json");

		assertThat(run.exitCode()).isZero();
		assertThat(new JSONObject(run.out()).getInt("schemaVersion"))
				.isEqualTo(1);
	}

	@Test
	void aFileWithValidationErrorsStillReportsAndExitsOne() throws IOException {
		Execution run = jpipe("--headless", "diagnostic", "-i",
				example("invalid/001_no_conclusion.jd"), "-f", "json");

		assertThat(run.exitCode()).isEqualTo(1);
		assertThat(new JSONObject(run.out()).getJSONArray("diagnostics"))
				.isNotEmpty();
	}

	// ── packaging-sensitive plumbing ─────────────────────────────────────────

	@Test
	void theManifestCarriesTheProjectVersion() throws IOException {
		// Proves the ManifestResourceTransformer kept Main-Class (the JAR ran
		// at all) and Implementation-Version.
		Execution run = jpipe("--version");

		assertThat(run.exitCode()).isZero();
		assertThat(run.out()).contains(System.getProperty("project.version"));
	}

	@Test
	void loggingSurvivesShading() throws IOException {
		// Log4j2's plugin registry is merged across JARs by a shade
		// transformer;
		// without it logging silently degrades to nothing.
		Execution run = jpipe("--headless", "--log-level=INFO", "diagnostic",
				"-i", MINIMAL);

		assertThat(run.exitCode()).isZero();
		assertThat(run.err()).contains("ChainCompiler");
	}

	@Test
	void doctorReportsOnTheToolsItProbes() throws IOException {
		// Exit code depends on whether Graphviz is installed on the machine
		// running the build, so only the report itself is asserted.
		Execution run = jpipe("--headless", "doctor");

		assertThat(run.exitCode()).isIn(Main.EXIT_OK, Main.EXIT_JPIPE_ERROR);
		assertThat(run.out()).contains("dot (Graphviz):");
	}

	@Test
	void theDiagnosticSchemaIsPackaged() throws IOException {
		// Served from the site at the $id URL the JSON report declares, so its
		// loss would break consumers without breaking any command.
		try (JarFile jar = new JarFile(new File(jarPath()))) {
			assertThat(jar.getEntry("schema/diagnostic-report-v1.schema.json"))
					.isNotNull();
		}
	}

	// ── helpers ──────────────────────────────────────────────────────────────

	/** Runs the shaded JAR with {@code args}, and captures what it produced. */
	private static Execution jpipe(String... args) throws IOException {
		List<String> command = new ArrayList<>(
				List.of(javaBinary(), "-jar", jarPath()));
		command.addAll(List.of(args));
		Process process = new ProcessBuilder(command).start();
		String out = read(process.getInputStream());
		String err = read(process.getErrorStream());
		return new Execution(waitFor(process), out, err);
	}

	private static int waitFor(Process process) {
		try {
			return process.waitFor();
		} catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(
					"interrupted while running the JAR");
		}
	}

	private static String read(InputStream stream) throws IOException {
		try (stream) {
			return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	/** The executable of the JVM running the build: always available. */
	private static String javaBinary() {
		return System.getProperty("java.home") + File.separator + "bin"
				+ File.separator + "java";
	}

	private static String jarPath() {
		return required("shaded.jar");
	}

	private static String example(String name) {
		return Path.of(required("examples.dir"), name).toString();
	}

	/** Reads a system property failsafe is expected to have set. */
	private static String required(String key) {
		String value = System.getProperty(key);
		if (value == null) {
			throw new IllegalStateException("'" + key
					+ "' is not set: run these tests through `mvn verify`");
		}
		return value;
	}
}
