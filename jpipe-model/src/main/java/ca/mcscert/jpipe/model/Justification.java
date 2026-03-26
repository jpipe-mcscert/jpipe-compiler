package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.CommonElement;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
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

	@Override
	public void addElement(CommonElement element) {
		if (locked) {
			throw new LockedJustificationException(getName());
		}
		super.addElement(element);
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

	/**
	 * Validates that every {@link ca.mcscert.jpipe.model.elements.Supportable}
	 * element has been assigned a supporter. Throws
	 * {@link IncompleteJustificationException} listing the ids of all unsupported
	 * elements.
	 */
	public void lock() {
		List<String> incomplete = new ArrayList<>();
		conclusions().stream().filter(c -> c.getSupport().isEmpty()).map(Conclusion::id).forEach(incomplete::add);
		subConclusions().stream().filter(sc -> sc.getSupport().isEmpty()).map(SubConclusion::id)
				.forEach(incomplete::add);
		strategies().stream().filter(s -> s.getSupport().isEmpty()).map(Strategy::id).forEach(incomplete::add);
		if (!incomplete.isEmpty()) {
			throw new IncompleteJustificationException(getName(), incomplete);
		}
		locked = true;
	}

	@Override
	public <R> R accept(JustificationVisitor<R> visitor) {
		R result = visitor.visit(this);
		getElements().forEach(e -> e.accept(visitor));
		return result;
	}
}
