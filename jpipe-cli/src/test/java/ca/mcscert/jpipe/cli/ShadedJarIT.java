package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarFile;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
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
	void anExportToStdoutStaysParseableWithoutHeadless() throws IOException {
		// The logo used to sit in front of every export written to stdout,
		// and every test here ran with --headless, so none saw it (#175).
		Execution run = jpipe("process", "-i", MINIMAL, "-m", "minimal", "-f",
				"json");

		assertThat(run.exitCode()).isZero();
		assertThat(new JSONObject(run.out()).getString("name"))
				.isEqualTo("minimal");
		assertThat(run.err()).contains("McMaster");
	}

	@Test
	void aFileWithValidationErrorsStillReportsAndExitsOne() throws IOException {
		Execution run = jpipe("--headless", "diagnostic", "-i",
				example("invalid/001_no_conclusion.jd"), "-f", "json");

		assertThat(run.exitCode()).isEqualTo(1);
		assertThat(new JSONObject(run.out()).getJSONArray("diagnostics"))
				.isNotEmpty();
	}

	@Test
	void aFileThatFailsFatallyStillReportsAndExitsOne() throws IOException {
		// The issue's own repro: an unresolvable load used to leave stdout
		// empty, which is the case a tool most needs to read (#154).
		Execution run = jpipe("--headless", "diagnostic", "-i",
				example("invalid/021_load_glob_invalid.jd"), "-f", "json");

		JSONObject report = new JSONObject(run.out());
		assertThat(run.exitCode()).isEqualTo(1);
		assertThat(report.getString("status")).isEqualTo("errors");
		assertThat(report.getJSONArray("diagnostics").getJSONObject(0)
				.getString("severity")).isEqualTo("fatal");
	}

	@Test
	void aModelThePythonExportRefusesFailsWithoutAStackTrace()
			throws IOException {
		// Two elements share an id: the export refuses the model, and used to
		// do so with a 40-line stack trace and exit 1 (#182).
		Execution run = jpipe("--headless", "process", "-i",
				example("invalid/000_duplicate_ids.jd"), "-m", "j", "-f",
				"python");

		assertThat(run.exitCode()).isEqualTo(Main.EXIT_JPIPE_ERROR);
		assertThat(run.out()).isEmpty();
		assertThat(run.err()).contains("[no-duplicate-ids]", "\"j:e\"")
				.doesNotContain("\tat ");
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
	void aSubcommandPrintsTheVersion() throws IOException {
		Execution run = jpipe("process", "-V");

		assertThat(run.exitCode()).isZero();
		assertThat(run.out()).contains(System.getProperty("project.version"));
	}

	@Test
	void noLogLevelHidesACompilationError() throws IOException {
		// Diagnostics used to reach the user only through the loggers that
		// --log-level tunes, so OFF left a bare "Compilation failed" (#186).
		Execution run = jpipe("--headless", "--log-level=OFF", "process", "-i",
				example("invalid/025_syntax_error.jd"), "-m", "broken");

		assertThat(run.exitCode()).isEqualTo(Main.EXIT_JPIPE_ERROR);
		assertThat(run.err())
				.containsPattern("025_syntax_error\\.jd:\\d+:\\d+: ");
	}

	@Test
	void loggingSurvivesShading() throws IOException {
		// Log4j2's plugin registry is merged across JARs by a shade
		// transformer;
		// without it logging silently degrades to nothing.
		Execution run = jpipe("--headless", "--log-level=INFO", "diagnostic",
				"-i", MINIMAL);

		assertThat(run.exitCode()).isZero();
		assertThat(run.err()).contains("Compiling [");
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

	// ── the launchers shipped next to the JAR ────────────────────────────────

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void thePosixLauncherRunsWithoutGraphviz(@TempDir Path dir)
			throws IOException {
		// The launcher used to exit 1 before Java started when `dot` was
		// missing, blocking `doctor` and every format that needs no Graphviz
		// (#186). Installed as the packages do: placeholders substituted, the
		// JAR next to it.
		Path launcher = dir.resolve("jpipe");
		Files.writeString(launcher,
				Files.readString(Path.of(required("launchers.dir"), "jpipe"))
						.replace("@@JAVA@@", javaBinary())
						.replace("@@PREFIX@@", dir.toString()));
		Files.createSymbolicLink(dir.resolve("jpipe.jar"), Path.of(jarPath()));
		Map<String, String> noGraphviz = Map.of("PATH",
				pathWith(dir, "java", "which").toString());

		Execution doctor = run(
				List.of("/bin/sh", launcher.toString(), "--headless", "doctor"),
				noGraphviz);
		Execution export = run(
				List.of("/bin/sh", launcher.toString(), "--headless", "process",
						"-i", MINIMAL, "-m", "minimal", "-f", "json"),
				noGraphviz);

		assertThat(doctor.exitCode()).isEqualTo(Main.EXIT_JPIPE_ERROR);
		assertThat(doctor.out()).contains("dot (Graphviz): NOT FOUND");
		assertThat(export.exitCode()).isZero();
		assertThat(new JSONObject(export.out()).getString("name"))
				.isEqualTo("minimal");
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void thePowerShellLauncherRunsWithoutGraphviz(@TempDir Path dir)
			throws IOException {
		Path pwsh = onPath("pwsh");
		assumeTrue(pwsh != null, "PowerShell is not installed");
		// Left unsubstituted, the script falls back to `java` on PATH and to
		// the JAR next to it.
		Path launcher = dir.resolve("jpipe.ps1");
		Files.copy(Path.of(required("launchers.dir"), "jpipe.ps1"), launcher);
		Files.createSymbolicLink(dir.resolve("jpipe.jar"), Path.of(jarPath()));
		Map<String, String> noGraphviz = Map.of("PATH",
				pathWith(dir, "java").toString());

		Execution doctor = run(
				List.of(pwsh.toString(), "-NoProfile", "-File",
						launcher.toString(), "--headless", "doctor"),
				noGraphviz);

		assertThat(doctor.exitCode()).isEqualTo(Main.EXIT_JPIPE_ERROR);
		assertThat(doctor.out()).contains("dot (Graphviz): NOT FOUND");
	}

	/**
	 * A directory, to use as the whole of {@code PATH}, holding links to the
	 * given tools and nothing else: notably no {@code dot}.
	 */
	private static Path pathWith(Path dir, String... tools) throws IOException {
		Path bin = Files.createDirectory(dir.resolve("bin"));
		for (String tool : tools) {
			Path target = tool.equals("java")
					? Path.of(javaBinary())
					: onPath(tool);
			assumeTrue(target != null, tool + " is not on PATH");
			Files.createSymbolicLink(bin.resolve(tool), target);
		}
		return bin;
	}

	/** Where {@code tool} is on the build's {@code PATH}, or {@code null}. */
	private static Path onPath(String tool) {
		for (String entry : System.getenv("PATH").split(File.pathSeparator)) {
			Path candidate = Path.of(entry, tool);
			if (Files.isExecutable(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	// ── helpers ──────────────────────────────────────────────────────────────

	/** Runs the shaded JAR with {@code args}, and captures what it produced. */
	private static Execution jpipe(String... args) throws IOException {
		List<String> command = new ArrayList<>(
				List.of(javaBinary(), "-jar", jarPath()));
		command.addAll(List.of(args));
		return run(command, Map.of());
	}

	/**
	 * Runs {@code command} with {@code env} on top of the build's environment,
	 * and captures what it produced.
	 */
	private static Execution run(List<String> command, Map<String, String> env)
			throws IOException {
		ProcessBuilder builder = new ProcessBuilder(command);
		builder.environment().putAll(env);
		Process process = builder.start();

		// Drain stderr in a virtual thread, as RenderWithDot does: reading the
		// two streams one after the other lets a chatty stderr fill its pipe
		// buffer and block the process before it can close stdout.
		AtomicReference<String> stderr = new AtomicReference<>("");
		Thread drain = Thread.ofVirtual().start(() -> {
			try {
				stderr.set(read(process.getErrorStream()));
			} catch (IOException _) {
				// Process died before stderr was fully read; safe to ignore.
			}
		});

		String out = read(process.getInputStream());
		int exitCode = waitFor(process);
		join(drain);
		return new Execution(exitCode, out, stderr.get());
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

	private static void join(Thread thread) {
		try {
			thread.join();
		} catch (InterruptedException _) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("interrupted while reading stderr");
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
