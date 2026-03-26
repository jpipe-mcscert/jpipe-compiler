package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.JustificationElement;
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
 */
public abstract sealed class JustificationModel<E extends JustificationElement> permits Justification, Template {

	private final String name;
	private Template parent = null;
	private final List<E> elements = new ArrayList<>();

	protected JustificationModel(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Optional<Template> getParent() {
		return Optional.ofNullable(parent);
	}

	public void setParent(Template parent) {
		this.parent = parent;
	}

	public void addElement(E element) {
		elements.add(element);
	}

	public List<E> getElements() {
		return Collections.unmodifiableList(elements);
	}

	public Optional<E> findById(String id) {
		return elements.stream().filter(e -> e.id().equals(id)).findFirst();
	}

	public <T extends E> List<T> elementsOfType(Class<T> type) {
		return elements.stream().filter(type::isInstance).map(type::cast).toList();
	}

	public abstract <R> R accept(JustificationVisitor<R> visitor);
}
