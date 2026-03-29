package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.model.elements.SupportLeaf;
import ca.mcscert.jpipe.model.validation.CompletenessValidator;
import ca.mcscert.jpipe.model.validation.ConsistencyValidator;
import ca.mcscert.jpipe.visitor.JustificationVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
 *
 * <p>
 * Use {@link #validate()} to check consistency and completeness. For
 * location-aware diagnostics in a compiler pipeline use
 * {@link ca.mcscert.jpipe.model.validation.ConsistencyValidator} and
 * {@link ca.mcscert.jpipe.model.validation.CompletenessValidator} directly.
 */
public abstract sealed class JustificationModel<E extends JustificationElement>
		permits Justification, Template {

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
	 * Sets the single required conclusion. Throws {@link IllegalStateException}
	 * if a conclusion has already been assigned.
	 */
	public void setConclusion(Conclusion conclusion) {
		if (this.conclusion != null) {
			throw new IllegalStateException(
					"Model '" + name + "' already has a conclusion");
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

	protected void setParent(Template parent) {
		if (this.parent != null) {
			throw new IllegalStateException(
					"Model '" + name + "' already has a parent template");
		}
		this.parent = parent;
	}

	/**
	 * Rejects {@link Conclusion} — use {@link #setConclusion(Conclusion)}
	 * instead.
	 */
	public void addElement(E element) {
		if (element instanceof Conclusion) {
			throw new IllegalArgumentException(
					"Use setConclusion() to assign the conclusion of a model");
		}
		elements.add(element);
	}

	/**
	 * Inlines {@code template} into this model: each template element is copied
	 * with a qualified id ({@code templateName:originalId}) and support edges
	 * between copied elements are re-wired. The model becomes self-contained —
	 * operators can mutate it freely without affecting the original template.
	 *
	 * <p>
	 * Whether a copy is added is controlled by {@link #includeInExpansion}:
	 * {@link Justification} excludes {@link AbstractSupport} copies (type
	 * constraint; abstract supports must be overridden by concrete elements);
	 * {@link Template} includes everything.
	 *
	 * <p>
	 * The template's conclusion is always copied as a {@link SubConclusion},
	 * becoming an intermediate goal rather than the top-level one.
	 */
	public void inline(Template template, String templateName) {
		setParent(template);

		// Step 1: build copy map (original plain id → copied element with
		// qualified id)
		Map<String, JustificationElement> copies = new LinkedHashMap<>();

		template.conclusion().ifPresent(tc -> {
			SubConclusion copy = new SubConclusion(templateName + ":" + tc.id(),
					tc.label());
			if (includeInExpansion(copy)) {
				copies.put(tc.id(), copy);
			}
		});

		for (JustificationElement elem : template.getElements()) {
			JustificationElement copy = qualifiedCopy(elem, templateName);
			if (includeInExpansion(copy)) {
				copies.put(elem.id(), copy);
			}
		}

		// Step 2: re-wire support edges between included copies only
		template.conclusion().ifPresent(tc -> tc.getSupport().ifPresent(s -> {
			if (copies.containsKey(tc.id()) && copies.containsKey(s.id())) {
				((SubConclusion) copies.get(tc.id()))
						.addSupport((Strategy) copies.get(s.id()));
			}
		}));

		for (SubConclusion sc : template.subConclusions()) {
			sc.getSupport().ifPresent(s -> {
				if (copies.containsKey(sc.id()) && copies.containsKey(s.id())) {
					((SubConclusion) copies.get(sc.id()))
							.addSupport((Strategy) copies.get(s.id()));
				}
			});
		}

		for (Strategy s : template.strategies()) {
			s.getSupport().ifPresent(leaf -> {
				String leafId = ((JustificationElement) leaf).id();
				if (copies.containsKey(s.id()) && copies.containsKey(leafId)) {
					((Strategy) copies.get(s.id()))
							.addSupport((SupportLeaf) copies.get(leafId));
				}
			});
		}

		// Step 3: add included copies to this model
		for (JustificationElement copy : copies.values()) {
			@SuppressWarnings("unchecked")
			E typed = (E) copy;
			addElement(typed);
		}
	}

	/**
	 * Returns true if the given element copy should be added to this model
	 * during template expansion. {@link Justification} returns false for
	 * {@link AbstractSupport}; {@link Template} returns true for all elements.
	 */
	protected abstract boolean includeInExpansion(JustificationElement copy);

	/**
	 * Validates this model for consistency and completeness without requiring a
	 * {@link Unit}. Useful for non-compiler consumers that build models
	 * programmatically.
	 *
	 * <p>
	 * All violations carry {@link SourceLocation#UNKNOWN} because source
	 * location data is only available when building through the compiler
	 * pipeline (see
	 * {@link ca.mcscert.jpipe.model.validation.ConsistencyValidator} and
	 * {@link ca.mcscert.jpipe.model.validation.CompletenessValidator}).
	 *
	 * @return unmodifiable list of violations; empty means valid.
	 */
	public List<Violation> validate() {
		List<Violation> violations = new ArrayList<>();
		violations.addAll(new ConsistencyValidator().validateModel(this));
		violations.addAll(new CompletenessValidator().validateModel(this));
		return Collections.unmodifiableList(violations);
	}

	/** Removes the element with the given id from the elements list. */
	public void removeElement(String id) {
		elements.removeIf(e -> e.id().equals(id));
	}

	public List<E> getElements() {
		return Collections.unmodifiableList(elements);
	}

	/**
	 * Searches both the conclusion field and the elements list, so callers do
	 * not need to know how the conclusion is stored.
	 */
	public Optional<E> findById(String id) {
		if (conclusion != null && conclusion.id().equals(id)) {
			@SuppressWarnings("unchecked")
			E c = (E) conclusion;
			return Optional.of(c);
		}
		Optional<E> exact = elements.stream().filter(e -> e.id().equals(id))
				.findFirst();
		if (exact.isPresent()) {
			return exact;
		}
		// Short-name suffix fallback: resolves a plain id to an inherited
		// qualified
		// element (e.g. "s" resolves to "t:s" after template expansion).
		String suffix = ":" + id;
		return elements.stream().filter(e -> e.id().endsWith(suffix))
				.findFirst();
	}

	public <T extends JustificationElement> List<T> elementsOfType(
			Class<T> type) {
		return elements.stream().filter(type::isInstance).map(type::cast)
				.toList();
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
		return visitSelf(visitor);
	}

	protected abstract <R> R visitSelf(JustificationVisitor<R> visitor);

	/** Returns the last colon-separated segment of a qualified id. */
	protected static String plainId(String id) {
		int colon = id.lastIndexOf(':');
		return colon >= 0 ? id.substring(colon + 1) : id;
	}

	/** Creates a copy of {@code elem} with id {@code prefix:originalId}. */
	protected static JustificationElement qualifiedCopy(
			JustificationElement elem, String prefix) {
		String qualifiedId = prefix + ":" + elem.id();
		return switch (elem) {
			case Evidence e -> new Evidence(qualifiedId, e.label());
			case Strategy s -> new Strategy(qualifiedId, s.label());
			case SubConclusion sc -> new SubConclusion(qualifiedId, sc.label());
			case Conclusion c -> new SubConclusion(qualifiedId, c.label());
			case AbstractSupport as ->
				new AbstractSupport(qualifiedId, as.label());
		};
	}
}
