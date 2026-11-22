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
import org.junit.jupiter.params.provider.ValueSource;

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

	@Test
	void anOutputThatIsTheInputIsRefused(@TempDir Path dir) throws IOException {
		// An in-place reformat used to erase the source (#174).
		Path source = Files.copy(Path.of(MINIMAL), dir.resolve("same.jd"));

		Execution run = jpipe("--headless", "process", "-f", "jpipe", "-i",
				source.toString(), "-m", "minimal", "-o", source.toString());

		assertThat(run.exitCode()).isEqualTo(2);
		assertThat(source).hasSameTextualContentAs(Path.of(MINIMAL));
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void anOutputToADeviceIsWrittenTo() throws IOException {
		// Renaming a temporary file over /dev/null is not permitted: the
		// output must be written in place.
		Execution run = jpipe("--headless", "process", "-i", MINIMAL, "-m",
				"minimal", "-f", "json", "-o", "/dev/null");

		assertThat(run.exitCode()).isZero();
		assertThat(run.err()).isEmpty();
	}

	@Test
	void aFailedRunKeepsThePreviousOutput(@TempDir Path dir)
			throws IOException {
		Path previous = Files.writeString(dir.resolve("out.dot"), "precious");

		Execution run = jpipe("--headless", "process", "-i", MINIMAL, "-m",
				"nope", "-f", "dot", "-o", previous.toString());

		assertThat(run.exitCode()).isEqualTo(Main.EXIT_JPIPE_ERROR);
		assertThat(previous).hasContent("precious");
	}

	@Test
	void anExportToANonUtf8TerminalIsStillUtf8(@TempDir Path dir)
			throws IOException {
		// Output to stdout used to follow the terminal's charset, so on a
		// non-UTF-8 console every non-ASCII letter became '?' (#179).
		// No em dash: org.json writes that one as an escape sequence.
		String label = "Le système est sûr, 安全";
		Path source = Files.writeString(dir.resolve("accents.jd"),
				"justification acc {\n    conclusion c is \"" + label
						+ "\"\n    strategy s is \"S\"\n    evidence e is \"E\"\n"
						+ "    s supports c\n    e supports s\n}\n",
				StandardCharsets.UTF_8);

		Execution run = run(
				List.of(javaBinary(), "-Dstdout.encoding=US-ASCII", "-jar",
						jarPath(), "--headless", "process", "-i",
						source.toString(), "-m", "acc", "-f", "json"),
				Map.of());

		assertThat(run.exitCode()).isZero();
		assertThat(run.out()).contains(label);
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

	@ParameterizedTest
	@ValueSource(strings = {"--log-level=OFF", "--log-level=ERROR",
			"--log-level=INFO"})
	void noLogLevelHidesACompilationError(String logLevel) throws IOException {
		// Diagnostics used to reach the user only through the loggers that
		// --log-level tunes, so OFF left a bare "Compilation failed" (#186).
		// Each is a bare line, which jpipe-vscode and scripts parse, whichever
		// logging back end the level starts (#216).
		Execution run = jpipe("--headless", logLevel, "process", "-i",
				example("invalid/025_syntax_error.jd"), "-m", "broken");

		assertThat(run.exitCode()).isEqualTo(Main.EXIT_JPIPE_ERROR);
		assertThat(run.err()).containsPattern(
				"(?m)^\\S*025_syntax_error\\.jd:\\d+:\\d+: error: ");
	}

	@ParameterizedTest
	@CsvSource({"--log-level=ERROR, false", "--log-level=INFO, true"})
	void log4jCoreStartsOnlyWhenTheLevelAsksForIt(String logLevel,
			boolean started) throws IOException {
		// log4j-core and its configuration used to start on every call, for a
		// large share of its start-up time (#216). The INFO run shows that the
		// check can see it start.
		Execution run = run(List.of(javaBinary(), "-Xlog:class+load=info",
				"-jar", jarPath(), "--headless", logLevel, "diagnostic", "-i",
				MINIMAL), Map.of());

		assertThat(run.exitCode()).isZero();
		assertThat(run.out()
				.contains("org.apache.logging.log4j.core.LoggerContext "))
				.isEqualTo(started);
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
	void thePosixLauncherCreatesAndUsesAnAotCache(@TempDir Path dir)
			throws IOException {
		// Each install channel runs `jpipe --create-aot-cache` once (#216).
		// It succeeds only if the training model compiles cleanly, so this
		// also keeps bin/aot-training.jd in step with the language.
		Path launcher = dir.resolve("jpipe");
		Files.writeString(launcher,
				Files.readString(Path.of(required("launchers.dir"), "jpipe"))
						.replace("@@JAVA@@", javaBinary())
						.replace("@@PREFIX@@", dir.toString()));
		Files.createSymbolicLink(dir.resolve("jpipe.jar"), Path.of(jarPath()));
		Map<String, String> path = Map.of("PATH",
				pathWith(dir, "java", "which", "mv", "rm", "cat").toString());
		List<String> createCache = List.of("/bin/sh", launcher.toString(),
				"--create-aot-cache");

		// Without its training model, the failure says why.
		Execution failed = run(createCache, path);
		boolean cacheAfterFailure = Files.exists(dir.resolve("jpipe.aot"));
		Files.copy(Path.of(required("launchers.dir"), "aot-training.jd"),
				dir.resolve("aot-training.jd"));
		List<String> export = List.of("/bin/sh", launcher.toString(),
				"--headless", "process", "-i", MINIMAL, "-m", "minimal", "-f",
				"json");

		Execution create = run(createCache, path);
		Execution cached = run(export, path);
		// A cache made stale by a Java upgrade must change nothing a caller
		// sees: no message, same output, same exit code.
		Files.writeString(dir.resolve("jpipe.aot"), "not a cache");
		Execution stale = run(export, path);

		assertThat(failed.exitCode()).isEqualTo(1);
		assertThat(failed.err()).contains("aot-training.jd",
				"could not create the AOT cache");
		assertThat(cacheAfterFailure).isFalse();
		assertThat(create.exitCode()).isZero();
		assertThat(cached.exitCode()).isZero();
		assertThat(new JSONObject(cached.out()).getString("name"))
				.isEqualTo("minimal");
		assertThat(stale.exitCode()).isZero();
		assertThat(stale.err()).isEmpty();
		assertThat(stale.out()).isEqualTo(cached.out());
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
