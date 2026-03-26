package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.CommonElement;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.exceptions.IncompleteJustificationException;
import ca.mcscert.jpipe.model.exceptions.LockedJustificationException;
import ca.mcscert.jpipe.visitor.JustificationVisitor;
import java.util.ArrayList;
import java.util.List;

/**
 * A concrete justification model. Only accepts {@link CommonElement}s — no
 * abstract supports.
 */
public final class Justification extends JustificationModel<CommonElement> {

	private boolean locked = false;

	public Justification(String name) {
		super(name);
	}

	public boolean isLocked() {
		return locked;
	}

	/** Also enforces the locked constraint before delegating to the base class. */
	@Override
	public void addElement(CommonElement element) {
		if (locked) {
			throw new LockedJustificationException(getName());
		}
		super.addElement(element);
	}

	/**
	 * Validates that the conclusion has been set and that every
	 * {@link ca.mcscert.jpipe.model.elements.Supportable} element has been assigned
	 * a supporter. Throws {@link IncompleteJustificationException} listing the ids
	 * of all unsupported elements.
	 */
	public void lock() {
		List<String> incomplete = new ArrayList<>();
		conclusion().filter(c -> c.getSupport().isEmpty()).map(Conclusion::id).ifPresent(incomplete::add);
		if (conclusion().isEmpty()) {
			incomplete.add("<conclusion>");
		}
		subConclusions().stream().filter(sc -> sc.getSupport().isEmpty()).map(SubConclusion::id)
				.forEach(incomplete::add);
		strategies().stream().filter(s -> s.getSupport().isEmpty()).map(Strategy::id).forEach(incomplete::add);
		if (!incomplete.isEmpty()) {
			throw new IncompleteJustificationException(getName(), incomplete);
		}
		locked = true;
	}

	@Override
	protected <R> R visitSelf(JustificationVisitor<R> visitor) {
		return visitor.visit(this);
	}
}
