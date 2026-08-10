package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.compiler.model.DiagnosticCodes;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Holds the published JSON Schema and the emitter to the same contract.
 *
 * <p>
 * The schema is validated here rather than at runtime: the document is built by
 * straight-line code from a typed {@code DiagnosticSnapshot}, so no external
 * input can make it structurally invalid — only a code change can, and that is
 * a build-time concern. This test is where that guarantee lives.
 *
 * <p>
 * The schema is loaded from the <em>classpath</em>, which also proves it is
 * packaged into the artifact for consumers to load the same way.
 */
class JsonDiagnosticReportSchemaTest {

	private static final String SCHEMA_RESOURCE = "/schema/diagnostic-report-v1.schema.json";

	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonSchema SCHEMA = loadSchema();

	private static JsonSchema loadSchema() {
		try (InputStream in = JsonDiagnosticReportSchemaTest.class
				.getResourceAsStream(SCHEMA_RESOURCE)) {
			if (in == null) {
				throw new IllegalStateException(
						"Schema not on the classpath: " + SCHEMA_RESOURCE);
			}
			return JsonSchemaFactory
					.getInstance(SpecVersion.VersionFlag.V202012).getSchema(in);
		} catch (IOException e) {
			throw new IllegalStateException("Cannot read the schema", e);
		}
	}

	// -------------------------------------------------------------------------
	// The schema is reachable the way consumers reach it
	// -------------------------------------------------------------------------

	@Test
	void schemaIsPackagedOnTheClasspath() {
		assertThat(JsonDiagnosticReportSchemaTest.class
				.getResourceAsStream(SCHEMA_RESOURCE)).isNotNull();
	}

	@Test
	void schemaDeclaresTheVersionTheEmitterWrites() throws IOException {
		JsonNode schema = MAPPER.readTree(JsonDiagnosticReportSchemaTest.class
				.getResourceAsStream(SCHEMA_RESOURCE));

		assertThat(schema.at("/properties/schemaVersion/const").asInt())
				.isEqualTo(JsonDiagnosticReport.SCHEMA_VERSION);
	}

	// -------------------------------------------------------------------------
	// Real output conforms
	// -------------------------------------------------------------------------

	@Test
	void emptyUnitConforms() {
		assertConforms(reportOf(new Unit("test.jd"),
				new CompilationContext("test.jd")));
	}

	@Test
	void unitWithDiagnosticsConforms() {
		CompilationContext ctx = new CompilationContext("test.jd");
		ctx.error(DiagnosticCodes.UNKNOWN_MODEL, 12, 4, "unknown model 'foo'");
		ctx.error("model construction failed");

		assertConforms(reportOf(new Unit("test.jd"), ctx));
	}

	/**
	 * No fatal fixture exists above because one cannot: {@code fire()}
	 * fast-fails on a fatal, so a report is never produced for a compilation
	 * that has one. The schema still admits the value — see
	 * {@link #schemaAdmitsFatalSeverityForFutureUse()}.
	 */
	@Test
	void aFatalPreventsAnyReportFromBeingProduced() {
		CompilationContext ctx = new CompilationContext("test.jd");
		ctx.fatal("unrecoverable");
		Unit unit = new Unit("test.jd");

		assertThatThrownBy(() -> reportOf(unit, ctx))
				.isInstanceOf(CompilationException.class);
	}

	@Test
	void unitWithTemplateImplementorAliasesAndSymbolsConforms() {
		Unit unit = new Unit("test.jd");
		Template t = new Template("base");
		t.setConclusion(new Conclusion("tc", "Template conclusion"));
		t.addElement(new Strategy("ts", "Template strategy"));
		unit.add(t);
		unit.recordLocation("base", new SourceLocation("test.jd", 3, 8));
		unit.recordLocation("base", "tc", new SourceLocation("test.jd", 4, 2));

		Justification j = new Justification("impl");
		j.inline(t, "base");
		j.addElement(new Evidence("ev", "Some evidence"));
		unit.add(j);
		unit.recordLocation("impl", new SourceLocation("test.jd", 10, 0));
		unit.recordAlias("impl", "old", "new");

		assertConforms(reportOf(unit, new CompilationContext("test.jd")));
	}

	/**
	 * The whole example corpus, compiled for real. Sources that fail fatally
	 * produce no report at all (documented behaviour), so they are skipped
	 * rather than asserted on.
	 */
	@ParameterizedTest(name = "{0}")
	@MethodSource("exampleSources")
	void everyExampleConformsToTheSchema(Path source) throws IOException {
		Transformation<InputStream, Unit> pipeline = CompilerFactory
				.parsingChain().andThen(CompilerFactory.unitBuilder())
				.asTransformation();
		CompilationContext ctx = new CompilationContext(source.toString());
		Unit unit;
		try (FileInputStream stream = new FileInputStream(source.toFile())) {
			unit = pipeline.fire(stream, ctx);
		} catch (CompilationException _) {
			return; // fatal: no report is produced, in either format
		}

		assertConforms(reportOf(unit, ctx));
	}

	static Stream<Path> exampleSources() throws IOException {
		Path root = Path.of(System.getProperty("examples.dir"));
		try (Stream<Path> paths = Files.walk(root)) {
			return paths.filter(p -> p.toString().endsWith(".jd"))
					.sorted(Comparator.naturalOrder()).toList().stream();
		}
	}

	// -------------------------------------------------------------------------
	// The schema actually bites
	// -------------------------------------------------------------------------
	// A schema that accepts everything would make every test above vacuous.
	// These pin the constraints that matter most.

	@Test
	void schemaRejectsAnUnexpectedKey() throws IOException {
		JsonNode report = MAPPER.readTree(
				reportOf(new Unit("test.jd"), new CompilationContext("t.jd")));
		((ObjectNode) report).put("oops", 1);

		assertThat(SCHEMA.validate(report)).isNotEmpty();
	}

	@Test
	void schemaRejectsABracketedCode() throws IOException {
		JsonNode report = MAPPER.readTree(reportWithCode("[unknown-model]"));

		assertThat(SCHEMA.validate(report)).isNotEmpty();
	}

	@Test
	void schemaAcceptsABareCode() throws IOException {
		JsonNode report = MAPPER.readTree(reportWithCode("unknown-model"));

		assertThat(SCHEMA.validate(report)).isEmpty();
	}

	/**
	 * The value is reserved rather than reachable today: a fatal aborts before
	 * any report is rendered. Pinned so the schema stays ready if that
	 * limitation is lifted.
	 */
	@Test
	void schemaAdmitsFatalSeverityForFutureUse() throws IOException {
		JsonNode report = MAPPER.readTree(reportWithCode("unknown-model"));
		((ObjectNode) report.at("/diagnostics/0")).put("severity", "fatal");

		assertThat(SCHEMA.validate(report)).isEmpty();
	}

	@Test
	void schemaRejectsAnUnknownSeverity() throws IOException {
		JsonNode report = MAPPER.readTree(reportWithCode("unknown-model"));
		((ObjectNode) report.at("/diagnostics/0")).put("severity", "warning");

		assertThat(SCHEMA.validate(report)).isNotEmpty();
	}

	@Test
	void schemaRejectsASynthesizedSymbolCarryingALocation() throws IOException {
		JsonNode report = MAPPER.readTree(
				reportOf(unitWithOneSymbol(), new CompilationContext("t.jd")));
		ObjectNode symbol = (ObjectNode) report.at("/models/0/symbols/0");
		symbol.put("synthesized", true);

		assertThat(SCHEMA.validate(report))
				.as("a synthesized element must not carry a location")
				.isNotEmpty();
	}

	@Test
	void schemaRejectsADiagnosticWithALineButNoColumn() throws IOException {
		CompilationContext ctx = new CompilationContext("t.jd");
		ctx.error(DiagnosticCodes.UNKNOWN_MODEL, 3, 1, "unknown model 'x'");
		JsonNode report = MAPPER.readTree(reportOf(new Unit("t.jd"), ctx));
		((ObjectNode) report.at("/diagnostics/0")).remove("column");

		assertThat(SCHEMA.validate(report)).isNotEmpty();
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private void assertConforms(String json) {
		Set<ValidationMessage> errors;
		try {
			errors = SCHEMA.validate(MAPPER.readTree(json));
		} catch (IOException e) {
			throw new AssertionError("report is not valid JSON", e);
		}
		assertThat(errors).as("schema violations in:%n%s", json).isEmpty();
	}

	private static String reportOf(Unit unit, CompilationContext ctx) {
		return new CollectDiagnostics().andThen(new JsonDiagnosticReport())
				.fire(unit, ctx);
	}

	private static String reportWithCode(String code) {
		CompilationContext ctx = new CompilationContext("t.jd");
		ctx.error(code, 1, 0, "a message");
		return reportOf(new Unit("t.jd"), ctx);
	}

	private static Unit unitWithOneSymbol() {
		Unit unit = new Unit("t.jd");
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "A conclusion"));
		unit.add(j);
		unit.recordLocation("j", new SourceLocation("t.jd", 1, 0));
		unit.recordLocation("j", "c", new SourceLocation("t.jd", 2, 3));
		return unit;
	}
}
