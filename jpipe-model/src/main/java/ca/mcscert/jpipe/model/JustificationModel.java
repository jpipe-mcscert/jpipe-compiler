package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.visitor.JustificationVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Abstract base for all named justification models. Sealed to exactly two
 * subtypes: {@link Justification} (concrete) and {@link Template}
 * (abstract/reusable).
 *
 * <p>
 * The type parameter {@code E} constrains which elements can be added:
 * {@link Justification} accepts only
 * {@link ca.mcscert.jpipe.model.elements.CommonElement}, while {@link Template}
 * accepts any {@link JustificationElement} including
 * {@link ca.mcscert.jpipe.model.elements.AbstractSupport}.
 *
 * <p>
 * Every model has exactly one {@link Conclusion}, set via
 * {@link #setConclusion(Conclusion)} and accessed via {@link #conclusion()}.
 * Passing a {@link Conclusion} to {@link #addElement(JustificationElement)} is
 * rejected.
 */
public abstract sealed class JustificationModel<E extends JustificationElement> permits Justification, Template {

	private final String name;
	private Conclusion conclusion;
	private Template parent = null;
	private final List<E> elements = new ArrayList<>();

	protected JustificationModel(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	/**
	 * Sets the single required conclusion. Throws {@link IllegalStateException} if
	 * a conclusion has already been assigned.
	 */
	public void setConclusion(Conclusion conclusion) {
		if (this.conclusion != null) {
			throw new IllegalStateException("Model '" + name + "' already has a conclusion");
		}
		this.conclusion = conclusion;
	}

	/** Returns the conclusion, if one has been set. */
	public Optional<Conclusion> conclusion() {
		return Optional.ofNullable(conclusion);
	}

	public Optional<Template> getParent() {
		return Optional.ofNullable(parent);
	}

	public void setParent(Template parent) {
		if (this.parent != null) {
			throw new IllegalStateException("Model '" + name + "' already has a parent template");
		}
		this.parent = parent;
	}

	/**
	 * Rejects {@link Conclusion} — use {@link #setConclusion(Conclusion)} instead.
	 */
	public void addElement(E element) {
		if (element instanceof Conclusion) {
			throw new IllegalArgumentException("Use setConclusion() to assign the conclusion of a model");
		}
		elements.add(element);
	}

	public List<E> getElements() {
		return Collections.unmodifiableList(elements);
	}

	/**
	 * Searches both the conclusion field and the elements list, so callers do not
	 * need to know how the conclusion is stored.
	 */
	public Optional<E> findById(String id) {
		if (conclusion != null && conclusion.id().equals(id)) {
			@SuppressWarnings("unchecked")
			E c = (E) conclusion;
			return Optional.of(c);
		}
		return elements.stream().filter(e -> e.id().equals(id)).findFirst();
	}

	public <T extends JustificationElement> List<T> elementsOfType(Class<T> type) {
		return elements.stream().filter(type::isInstance).map(type::cast).toList();
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

	public final <R> R accept(JustificationVisitor<R> visitor) {
		R result = visitSelf(visitor);
		conclusion().ifPresent(c -> c.accept(visitor));
		getElements().forEach(e -> e.accept(visitor));
		return result;
	}

	protected abstract <R> R visitSelf(JustificationVisitor<R> visitor);
}
