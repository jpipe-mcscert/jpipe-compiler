package ca.mcscert.jpipe.compiler.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ca.mcscert.jpipe.compiler.CompilerFactory;
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
import ca.mcscert.jpipe.visitor.PythonExporter;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import java.io.FileInputStream;
import java.io.InputStream;

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

	@When("I export the current model to DOT format")
	public void iExportTheCurrentModelToDotFormat() {
		dotOutput = new DotExporter().export(currentModel);
	}

	@Then("the DOT output contains a node with id {string}")
	public void theDotOutputContainsANodeWithId(String id) {
		assertThat(dotOutput).contains("id=\"" + id + "\"");
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
				.extracting(Diagnostic::message)
				.anySatisfy(msg -> assertThat(msg).contains("[" + rule + "]"));
	}
}
