package ca.mcscert.jpipe.compiler.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.io.FileInputStream;
import java.io.InputStream;

/** Step definitions for end-to-end compilation scenarios. */
public class CompilationSteps {

	private String sourcePath;
	private CompilationContext ctx;
	private Unit unit;
	private Exception compilationError;
	private JustificationModel<?> currentModel;

	@Given("the source file {string}")
	public void theSourceFile(String filename) {
		sourcePath = System.getProperty("examples.dir") + "/" + filename;
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
		Strategy strategy = (Strategy) currentModel.findById(strategyId)
				.orElseThrow(() -> new AssertionError(
						"No strategy with id: " + strategyId));
		assertThat(strategy.getSupport()).isPresent().hasValueSatisfying(
				sl -> assertThat(((JustificationElement) sl).id())
						.isEqualTo(abstractSupportId));
	}

	@Then("the evidence {string} supports the strategy {string}")
	public void evidenceSupportsStrategy(String evidenceId, String strategyId) {
		Strategy strategy = (Strategy) currentModel.findById(strategyId)
				.orElseThrow(() -> new AssertionError(
						"No strategy with id: " + strategyId));
		assertThat(strategy.getSupport()).isPresent().hasValueSatisfying(
				sl -> assertThat(((JustificationElement) sl).id())
						.isEqualTo(evidenceId));
	}
}
