package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.model.elements.SupportLeaf;
import ca.mcscert.jpipe.model.exceptions.IncompleteJustificationException;
import ca.mcscert.jpipe.model.exceptions.LockedModelException;
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
 */
public abstract sealed class JustificationModel<E extends JustificationElement> permits Justification, Template {

	private final String name;
	private boolean locked = false;
	private Conclusion conclusion;
	private Template parent = null;
	private final List<E> elements = new ArrayList<>();

	protected JustificationModel(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public boolean isLocked() {
		return locked;
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

	protected void setParent(Template parent) {
		if (this.parent != null) {
			throw new IllegalStateException("Model '" + name + "' already has a parent template");
		}
		this.parent = parent;
	}

	/**
	 * Rejects {@link Conclusion} — use {@link #setConclusion(Conclusion)} instead.
	 * Throws {@link LockedModelException} if the model has been locked.
	 */
	public void addElement(E element) {
		if (locked) {
			throw new LockedModelException(name);
		}
		if (element instanceof Conclusion) {
			throw new IllegalArgumentException("Use setConclusion() to assign the conclusion of a model");
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

		// Step 1: build copy map (original plain id → copied element with qualified id)
		Map<String, JustificationElement> copies = new LinkedHashMap<>();

		template.conclusion().ifPresent(tc -> {
			SubConclusion copy = new SubConclusion(templateName + ":" + tc.id(), tc.label());
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
				((SubConclusion) copies.get(tc.id())).addSupport((Strategy) copies.get(s.id()));
			}
		}));

		for (SubConclusion sc : template.subConclusions()) {
			sc.getSupport().ifPresent(s -> {
				if (copies.containsKey(sc.id()) && copies.containsKey(s.id())) {
					((SubConclusion) copies.get(sc.id())).addSupport((Strategy) copies.get(s.id()));
				}
			});
		}

		for (Strategy s : template.strategies()) {
			s.getSupport().ifPresent(leaf -> {
				String leafId = ((JustificationElement) leaf).id();
				if (copies.containsKey(s.id()) && copies.containsKey(leafId)) {
					((Strategy) copies.get(s.id())).addSupport((SupportLeaf) copies.get(leafId));
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
	 * Returns true if the given element copy should be added to this model during
	 * template expansion. {@link Justification} returns false for
	 * {@link AbstractSupport}; {@link Template} returns true for all elements.
	 */
	protected abstract boolean includeInExpansion(JustificationElement copy);

	/**
	 * Validates completeness and locks the model against further modification.
	 * Subclasses may add extra validation rules by overriding
	 * {@link #validateForLock(List)}.
	 */
	public final void lock() {
		List<String> incomplete = new ArrayList<>();
		conclusion().filter(c -> c.getSupport().isEmpty()).map(Conclusion::id).ifPresent(incomplete::add);
		if (conclusion().isEmpty()) {
			incomplete.add("<conclusion>");
		}
		subConclusions().stream().filter(sc -> sc.getSupport().isEmpty()).map(SubConclusion::id)
				.forEach(incomplete::add);
		strategies().stream().filter(s -> s.getSupport().isEmpty()).map(Strategy::id).forEach(incomplete::add);
		validateForLock(incomplete);
		if (!incomplete.isEmpty()) {
			throw new IncompleteJustificationException(name, incomplete);
		}
		locked = true;
	}

	/**
	 * Hook for subclass-specific lock validation. Implementations should append the
	 * ids (or symbolic names) of any incomplete elements to {@code incomplete}.
	 */
	protected void validateForLock(List<String> incomplete) {
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
		Optional<E> exact = elements.stream().filter(e -> e.id().equals(id)).findFirst();
		if (exact.isPresent()) {
			return exact;
		}
		// Short-name suffix fallback: resolves a plain id to an inherited qualified
		// element (e.g. "s" resolves to "t:s" after template expansion).
		String suffix = ":" + id;
		return elements.stream().filter(e -> e.id().endsWith(suffix)).findFirst();
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
		return visitSelf(visitor);
	}

	protected abstract <R> R visitSelf(JustificationVisitor<R> visitor);

	/** Returns the last colon-separated segment of a qualified id. */
	protected static String plainId(String id) {
		int colon = id.lastIndexOf(':');
		return colon >= 0 ? id.substring(colon + 1) : id;
	}

	/** Creates a copy of {@code elem} with id {@code prefix:originalId}. */
	protected static JustificationElement qualifiedCopy(JustificationElement elem, String prefix) {
		String qualifiedId = prefix + ":" + elem.id();
		return switch (elem) {
			case Evidence e -> new Evidence(qualifiedId, e.label());
			case Strategy s -> new Strategy(qualifiedId, s.label());
			case SubConclusion sc -> new SubConclusion(qualifiedId, sc.label());
			case Conclusion c -> new SubConclusion(qualifiedId, c.label());
			case AbstractSupport as -> new AbstractSupport(qualifiedId, as.label());
		};
	}
}
