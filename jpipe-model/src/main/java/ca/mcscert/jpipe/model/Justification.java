package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.CommonElement;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.visitor.JustificationVisitor;

/**
 * A concrete justification model. Only accepts {@link CommonElement}s — no
 * abstract supports.
 */
public final class Justification extends JustificationModel<CommonElement> {

	public Justification(String name) {
		super(name);
	}

	/**
	 * Includes all template elements — including {@link AbstractSupport} copies —
	 * so that override commands can find and replace them after inlining.
	 */
	@Override
	protected boolean includeInExpansion(JustificationElement copy) {
		return true;
	}

	@Override
	protected <R> R visitSelf(JustificationVisitor<R> visitor) {
		return visitor.visit(this);
	}
}
