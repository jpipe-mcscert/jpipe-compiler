package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.CommonElement;
import ca.mcscert.jpipe.visitor.JustificationVisitor;

/**
 * A concrete justification model, whose own elements are
 * {@link CommonElement}s.
 *
 * <p>
 * A justification that implements a template is the one exception: template
 * expansion ({@link #inline}) also copies the template's
 * {@link AbstractSupport} placeholders into it, so that override commands can
 * find and replace them. They are stored through an unchecked cast, so the
 * element list may hold an {@link AbstractSupport} until it is overridden; read
 * them with {@link #elementsOfType(Class)} rather than as
 * {@link CommonElement}s. One left unreplaced is reported by the completeness
 * check ({@code no-abstract-support}).
 */
public final class Justification extends JustificationModel<CommonElement> {

	public Justification(String name) {
		super(name);
	}

	@Override
	protected <R> R visitSelf(JustificationVisitor<R> visitor) {
		return visitor.visit(this);
	}
}
