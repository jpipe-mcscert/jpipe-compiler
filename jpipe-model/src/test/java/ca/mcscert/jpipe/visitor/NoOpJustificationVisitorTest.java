package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import org.junit.jupiter.api.Test;

class NoOpJustificationVisitorTest {

	private static final class ConcreteVisitor
			extends
				NoOpJustificationVisitor<String> {
	}

	private final ConcreteVisitor visitor = new ConcreteVisitor();

	@Test
	void visitUnitReturnsNull() {
		assertThat(visitor.visit(new Unit("src"))).isNull();
	}

	@Test
	void visitJustificationReturnsNull() {
		assertThat(visitor.visit(new Justification("j"))).isNull();
	}

	@Test
	void visitTemplateReturnsNull() {
		assertThat(visitor.visit(new Template("t"))).isNull();
	}

	@Test
	void visitConclusionReturnsNull() {
		assertThat(visitor.visit(new Conclusion("c", "label"))).isNull();
	}

	@Test
	void visitSubConclusionReturnsNull() {
		assertThat(visitor.visit(new SubConclusion("sc", "label"))).isNull();
	}

	@Test
	void visitStrategyReturnsNull() {
		assertThat(visitor.visit(new Strategy("s", "label"))).isNull();
	}

	@Test
	void visitEvidenceReturnsNull() {
		assertThat(visitor.visit(new Evidence("e", "label"))).isNull();
	}

	@Test
	void visitAbstractSupportReturnsNull() {
		assertThat(visitor.visit(new AbstractSupport("as", "label"))).isNull();
	}
}
