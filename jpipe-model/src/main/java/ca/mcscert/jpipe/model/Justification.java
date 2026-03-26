package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.CommonElement;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.visitor.JustificationVisitor;
import java.util.List;

/**
 * A concrete justification model. Only accepts {@link CommonElement}s — no
 * abstract supports.
 */
public final class Justification extends JustificationModel<CommonElement> {

	public Justification(String name) {
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

	@Override
	public <R> R accept(JustificationVisitor<R> visitor) {
		R result = visitor.visit(this);
		getElements().forEach(e -> e.accept(visitor));
		return result;
	}
}
