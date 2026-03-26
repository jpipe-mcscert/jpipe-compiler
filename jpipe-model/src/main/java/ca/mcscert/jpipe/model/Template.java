package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.visitor.JustificationVisitor;
import java.util.List;

/**
 * A reusable justification template (pattern). Accepts any element including
 * abstract supports.
 */
public final class Template extends JustificationModel<JustificationElement> {

	public Template(String name) {
		super(name);
	}

	public List<Conclusion> conclusions() {
		return elementsOfType(Conclusion.class);
	}

	public List<SubConclusion> subConclusions() {
		return elementsOfType(SubConclusion.class);
	}

	public List<Strategy> strategies() {
		return elementsOfType(Strategy.class);
	}

	public List<Evidence> evidence() {
		return elementsOfType(Evidence.class);
	}

	public List<AbstractSupport> abstractSupports() {
		return elementsOfType(AbstractSupport.class);
	}

	@Override
	public <R> R accept(JustificationVisitor<R> visitor) {
		R result = visitor.visit(this);
		getElements().forEach(e -> e.accept(visitor));
		return result;
	}
}
