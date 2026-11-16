package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.CommonElement;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.visitor.JustificationVisitor;

/**
 * A concrete justification model.
 *
 * <p>
 * The elements a justification declares are {@link CommonElement}s:
 * {@link Unit#addInto} refuses an {@link AbstractSupport}. Its element list
 * still holds {@link JustificationElement}s, because template expansion
 * ({@link #inline}) copies the template's {@link AbstractSupport} placeholders
 * into it, so that override commands can find and replace them. The type says
 * so, rather than an unchecked cast hiding it (#190). One left unreplaced is
 * reported by the completeness check ({@code no-abstract-support}).
 */
public final class Justification
		extends
			JustificationModel<JustificationElement> {

	public Justification(String name) {
		super(name);
	}

	@Override
	protected void addInlined(JustificationElement copy) {
		addElement(copy);
	}

	@Override
	protected <R> R visitSelf(JustificationVisitor<R> visitor) {
		return visitor.visit(this);
	}
}
