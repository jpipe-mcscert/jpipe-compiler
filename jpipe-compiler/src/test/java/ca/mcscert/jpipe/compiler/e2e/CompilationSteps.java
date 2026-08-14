package ca.mcscert.jpipe.compiler.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.compiler.DiagnosticFormat;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.compiler.model.Diagnostic;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.visitor.DotExporter;
import ca.mcscert.jpipe.visitor.JsonExporter;
import ca.mcscert.jpipe.visitor.PythonExporter;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import ca.mcscert.jpipe.compiler.steps.transformations.CollectDiagnostics;
import ca.mcscert.jpipe.compiler.steps.transformations.DiagnosticReport;
import ca.mcscert.jpipe.compiler.steps.transformations.JsonDiagnosticReport;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;

/** Step definitions for end-to-end compilation scenarios. */
public class CompilationSteps {

	private static final String EXAMPLES_DIR = "examples.dir";

	private String sourcePath;
	private CompilationContext ctx;
	private Unit unit;
	private Exception compilationError;
	private JustificationModel<?> currentModel;
	private String dotOutput;
	private String pythonOutput;
	private String jsonOutput;
	private boolean reportedErrors;
	private String textReport;
	private JSONObject jsonReport;

	@Given("the source file {string}")
	public void theSourceFile(String filename) {
		sourcePath = System.getProperty(EXAMPLES_DIR) + "/" + filename;
	}

	@When("I compile it into a unit")
	public void iCompileItIntoAUnit() {
		Transformation<InputStream, Unit> pipeline = CompilerFactory
				.parsingChain().andThen(CompilerFactory.unitBuilder())
				.asTransformation();
		ctx = new CompilationContext(sourcePath);
		try (FileInputStream stream = new FileInputStream(sourcePath)) {
			unit = pipeline.fire(stream, ctx);
		} catch (Exception e) {
			compilationError = e;
		}
	}

	@Then("the compilation succeeds")
	public void theCompilationSucceeds() {
		assertThat(compilationError).isNull();
		assertThat(ctx.hasFatalErrors()).isFalse();
	}

	@Then("the compilation fails with a system error")
	public void theCompilationFailsWithASystemError() {
		assertThat(compilationError).isNotNull()
				.isNotInstanceOf(CompilationException.class)
				.isNotInstanceOf(UnsupportedOperationException.class);
	}

	@Then("the unit contains a justification named {string}")
	public void theUnitContainsAJustificationNamed(String name) {
		assertThat(unit.justifications())
				.extracting(JustificationModel::getName).contains(name);
		currentModel = unit.get(name);
	}

	@Then("the unit contains a template named {string}")
	public void theUnitContainsATemplateNamed(String name) {
		assertThat(unit.templates()).extracting(JustificationModel::getName)
				.contains(name);
		currentModel = unit.get(name);
	}

	@Then("it has a conclusion with id {string} and label {string}")
	public void itHasConclusion(String id, String label) {
		assertThat(currentModel.conclusion()).isPresent()
				.hasValueSatisfying(c -> {
					assertThat(c.id()).isEqualTo(id);
					assertThat(c.label()).isEqualTo(label);
				});
	}

	@Then("it has a strategy with id {string} and label {string}")
	public void itHasStrategy(String id, String label) {
		assertThat(currentModel.strategies())
				.extracting(JustificationElement::id,
						JustificationElement::label)
				.contains(tuple(id, label));
	}

	@Then("it has a sub-conclusion with id {string} and label {string}")
	public void itHasSubConclusion(String id, String label) {
		assertThat(currentModel.subConclusions())
				.extracting(JustificationElement::id,
						JustificationElement::label)
				.contains(tuple(id, label));
	}

	@Then("it has evidence with id {string} and label {string}")
	public void itHasEvidence(String id, String label) {
		assertThat(currentModel.evidence()).extracting(JustificationElement::id,
				JustificationElement::label).contains(tuple(id, label));
	}

	@Then("the strategy {string} supports the conclusion {string}")
	public void strategySupportsConclusion(String strategyId,
			String conclusionId) {
		Conclusion conclusion = (Conclusion) currentModel.findById(conclusionId)
				.orElseThrow(() -> new AssertionError(
						"No conclusion with id: " + conclusionId));
		assertThat(conclusion.getSupport()).isPresent().hasValueSatisfying(
				s -> assertThat(s.id()).isEqualTo(strategyId));
	}

	@Then("the strategy {string} supports the sub-conclusion {string}")
	public void strategySupportsSubConclusion(String strategyId,
			String subConclusionId) {
		SubConclusion subConclusion = (SubConclusion) currentModel
				.findById(subConclusionId).orElseThrow(() -> new AssertionError(
						"No sub-conclusion with id: " + subConclusionId));
		assertThat(subConclusion.getSupport()).isPresent().hasValueSatisfying(
				s -> assertThat(s.id()).isEqualTo(strategyId));
	}

	@Then("it has an abstract support with id {string} and label {string}")
	public void itHasAbstractSupport(String id, String label) {
		assertThat(currentModel.elementsOfType(AbstractSupport.class))
				.extracting(JustificationElement::id,
						JustificationElement::label)
				.contains(tuple(id, label));
	}

	@Then("the abstract support {string} supports the strategy {string}")
	public void abstractSupportSupportsStrategy(String abstractSupportId,
			String strategyId) {
		assertSupportsStrategy(abstractSupportId, strategyId);
	}

	@Then("the evidence {string} supports the strategy {string}")
	public void evidenceSupportsStrategy(String evidenceId, String strategyId) {
		assertSupportsStrategy(evidenceId, strategyId);
	}

	@Then("the sub-conclusion {string} supports the strategy {string}")
	public void subConclusionSupportsStrategy(String subConclusionId,
			String strategyId) {
		assertSupportsStrategy(subConclusionId, strategyId);
	}

	private void assertSupportsStrategy(String supporterId, String strategyId) {
		Strategy strategy = (Strategy) currentModel.findById(strategyId)
				.orElseThrow(() -> new AssertionError(
						"No strategy with id: " + strategyId));
		assertThat(strategy.getSupports())
				.extracting(sl -> ((JustificationElement) sl).id())
				.contains(supporterId);
	}

	@Then("the model {string} is declared at line {int}")
	public void theModelIsDeclaredAtLine(String modelName, int line) {
		assertThat(unit.locationOf(modelName).line()).isEqualTo(line);
	}

	@Then("the element {string} in model {string} is declared at line {int}")
	public void theElementInModelIsDeclaredAtLine(String elementId,
			String modelName, int line) {
		SourceLocation loc = unit.locationOf(modelName, elementId);
		assertThat(loc.isKnown())
				.as("location of %s/%s should be known", modelName, elementId)
				.isTrue();
		assertThat(loc.line()).isEqualTo(line);
	}

	// -------------------------------------------------------------------------
	// Diagnostic reports
	// -------------------------------------------------------------------------

	@When("I produce a text diagnostic report")
	public void iProduceATextDiagnosticReport() {
		textReport = new CollectDiagnostics().andThen(new DiagnosticReport())
				.fire(unit, ctx);
	}

	@When("I produce a JSON diagnostic report")
	public void iProduceAJsonDiagnosticReport() {
		jsonReport = new JSONObject(new CollectDiagnostics()
				.andThen(new JsonDiagnosticReport()).fire(unit, ctx));
	}

	/**
	 * Runs the {@code diagnostic} command's own compiler, which reports on a
	 * compilation whether it completed or aborted — unlike the steps above,
	 * which need a unit and so cannot describe a fatal (#154).
	 */
	@When("I run the diagnostic compiler with format {string}")
	public void iRunTheDiagnosticCompilerWithFormat(String format)
			throws IOException {
		DiagnosticFormat target = DiagnosticFormat
				.valueOf(format.toUpperCase(Locale.ROOT));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		reportedErrors = CompilerFactory.buildDiagnosticCompiler(target, out)
				.compile(sourcePath, CompilationConfig.STDOUT);
		String rendered = out.toString(StandardCharsets.UTF_8);
		if (target == DiagnosticFormat.JSON) {
			jsonReport = new JSONObject(rendered);
		} else {
			textReport = rendered;
		}
	}

	@Then("the report signals errors")
	public void theReportSignalsErrors() {
		assertThat(reportedErrors).isTrue();
	}

	@Then("the JSON report has a diagnostic with severity {string}")
	public void theJsonReportHasADiagnosticWithSeverity(String severity) {
		assertThat(severitiesIn(jsonReport)).contains(severity);
	}

	@Then("the JSON report describes no model")
	public void theJsonReportDescribesNoModel() {
		assertThat(jsonReport.getJSONArray("models")).isEmpty();
	}

	@Then("the text report contains {string}")
	public void theTextReportContains(String fragment) {
		assertThat(textReport).contains(fragment);
	}

	@Then("the JSON report status is {string}")
	public void theJsonReportStatusIs(String status) {
		assertThat(jsonReport.getString("status")).isEqualTo(status);
	}

	@Then("the JSON report declares a schema version")
	public void theJsonReportDeclaresASchemaVersion() {
		assertThat(jsonReport.getInt("schemaVersion")).isPositive();
	}

	@Then("the JSON report has a diagnostic with code {string}")
	public void theJsonReportHasADiagnosticWithCode(String code) {
		assertThat(codesIn(jsonReport)).contains(code);
	}

	@Then("no JSON diagnostic message contains its own code")
	public void noJsonDiagnosticMessageContainsItsOwnCode() {
		JSONArray diagnostics = jsonReport.getJSONArray("diagnostics");
		for (int i = 0; i < diagnostics.length(); i++) {
			JSONObject d = diagnostics.getJSONObject(i);
			if (d.has("code")) {
				assertThat(d.getString("message"))
						.doesNotContain(d.getString("code"));
			}
		}
	}

	@Then("the JSON report describes a {string} named {string}")
	public void theJsonReportDescribesAModelNamed(String kind, String name) {
		JSONArray models = jsonReport.getJSONArray("models");
		boolean found = false;
		for (int i = 0; i < models.length(); i++) {
			JSONObject model = models.getJSONObject(i);
			found = found || (name.equals(model.getString("name"))
					&& kind.equals(model.getString("kind")));
		}
		assertThat(found).as("a %s named %s", kind, name).isTrue();
	}

	@Then("both reports agree on the number of diagnostics")
	public void bothReportsAgreeOnTheNumberOfDiagnostics() {
		assertThat(jsonReport.getJSONArray("diagnostics").length())
				.isEqualTo(ctx.diagnostics().size());
	}

	private static List<String> severitiesIn(JSONObject report) {
		JSONArray diagnostics = report.getJSONArray("diagnostics");
		List<String> severities = new ArrayList<>();
		for (int i = 0; i < diagnostics.length(); i++) {
			severities.add(diagnostics.getJSONObject(i).getString("severity"));
		}
		return severities;
	}

	private static List<String> codesIn(JSONObject report) {
		JSONArray diagnostics = report.getJSONArray("diagnostics");
		List<String> codes = new ArrayList<>();
		for (int i = 0; i < diagnostics.length(); i++) {
			JSONObject d = diagnostics.getJSONObject(i);
			if (d.has("code")) {
				codes.add(d.getString("code"));
			}
		}
		return codes;
	}

	@When("I export the current model to DOT format")
	public void iExportTheCurrentModelToDotFormat() {
		dotOutput = new DotExporter().export(currentModel);
	}

	@Then("the DOT output contains a node with id {string}")
	public void theDotOutputContainsANodeWithId(String id) {
		assertThat(dotOutput).contains("id=\"" + id + "\"");
	}

	@When("I export the current model to JSON format")
	public void iExportTheCurrentModelToJsonFormat() {
		jsonOutput = new JsonExporter().export(currentModel);
	}

	@Then("the JSON output contains {string}")
	public void theJsonOutputContains(String fragment) {
		assertThat(jsonOutput).contains(fragment);
	}

	@When("I export the current model to Python format")
	public void iExportTheCurrentModelToPythonFormat() {
		pythonOutput = new PythonExporter().export(currentModel);
	}

	@Then("the Python output contains a method named {string}")
	public void thePythonOutputContainsAMethodNamed(String name) {
		assertThat(pythonOutput).contains("def " + name + "(");
	}

	@Then("the Python output has @jpipe_link for id {string} active")
	public void thePythonOutputHasJpipeLinkActive(String qualifiedId) {
		assertThat(pythonOutput)
				.contains("@jpipe_link(\"" + qualifiedId + "\")")
				.doesNotContain("# @jpipe_link(\"" + qualifiedId + "\")");
	}

	@Then("the Python output has @jpipe_link for id {string} commented out")
	public void thePythonOutputHasJpipeLinkCommentedOut(String qualifiedId) {
		assertThat(pythonOutput)
				.contains("# @jpipe_link(\"" + qualifiedId + "\")");
	}

	@Then("the Python output declares {string} before {string}")
	public void thePythonOutputDeclaresBefore(String first, String second) {
		assertThat(pythonOutput).containsSubsequence("def " + first + "(",
				"def " + second + "(");
	}

	@Then("the Python output has a namespace section for {string}")
	public void thePythonOutputHasANamespaceSectionFor(String namespace) {
		String edge = "###" + " ".repeat(namespace.length()) + "###";
		assertThat(pythonOutput)
				.contains(edge + "\n## " + namespace + " ##\n" + edge + "\n");
	}

	@Then("the Python output has no @jpipe_link for id {string}")
	public void thePythonOutputHasNoJpipeLinkFor(String qualifiedId) {
		assertThat(pythonOutput)
				.doesNotContain("@jpipe_link(\"" + qualifiedId + "\")");
	}

	@Then("the compilation has validation errors")
	public void theCompilationHasValidationErrors() {
		assertThat(ctx.hasErrors()).isTrue();
	}

	@Then("the compilation fails with a fatal error")
	public void theCompilationFailsWithAFatalError() {
		assertThat(ctx.hasFatalErrors()).isTrue();
	}

	@Then("a fatal error mentions {string}")
	public void aFatalErrorMentions(String text) {
		assertThat(ctx.diagnostics()).filteredOn(Diagnostic::isFatal)
				.extracting(Diagnostic::message)
				.anySatisfy(msg -> assertThat(msg).contains(text));
	}

	@Then("a validation error is reported for rule {string}")
	public void aValidationErrorIsReportedForRule(String rule) {
		assertThat(ctx.diagnostics()).filteredOn(Diagnostic::isError)
				.extracting(Diagnostic::code).contains(rule);
	}

	@Then("a validation error mentions {string}")
	public void aValidationErrorMentions(String text) {
		assertThat(ctx.diagnostics()).filteredOn(Diagnostic::isError)
				.extracting(Diagnostic::message)
				.anySatisfy(msg -> assertThat(msg).contains(text));
	}
}
