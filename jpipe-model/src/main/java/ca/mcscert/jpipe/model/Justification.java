package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
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
	 * Excludes {@link AbstractSupport} copies from expansion: they cannot be added
	 * to a {@link Justification} and must be resolved by overriding with a concrete
	 * element.
	 */
	@Override
	protected boolean includeInExpansion(JustificationElement copy) {
		return !(copy instanceof AbstractSupport);
	}

	@Override
	protected <R> R visitSelf(JustificationVisitor<R> visitor) {
		return visitor.visit(this);
	}
}
