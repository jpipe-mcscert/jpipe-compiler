package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.JustificationElement;
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

	public List<AbstractSupport> abstractSupports() {
		return elementsOfType(AbstractSupport.class);
	}

	@Override
	protected <R> R visitSelf(JustificationVisitor<R> visitor) {
		return visitor.visit(this);
	}
}
