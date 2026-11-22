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
import ca.mcscert.jpipe.util.Aliases;
import ca.mcscert.jpipe.util.QualifiedIds;
import ca.mcscert.jpipe.visitor.JustificationVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * Abstract base for all named justification models. Sealed to exactly two
 * subtypes: {@link Justification} (concrete) and {@link Template}
 * (abstract/reusable).
 *
 * <p>
 * Both kinds of model hold any {@link JustificationElement}: a
 * {@link Justification} that implements a template holds the template's
 * {@link ca.mcscert.jpipe.model.elements.AbstractSupport} placeholders until
 * they are overridden. What a justification may <em>declare</em> is narrower,
 * and enforced where elements are declared ({@link Unit#addInto}) and by the
 * completeness check ({@code no-abstract-support}).
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
public abstract sealed class JustificationModel
		permits Justification, Template {

	private final String name;
	private Conclusion conclusion;
	private Template parent = null;
	private final List<JustificationElement> elements = new ArrayList<>();
	private final Map<String, String> aliases = new LinkedHashMap<>();
	private final Set<String> unifiedIds = new LinkedHashSet<>();
	private Composition composition = null;

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
	 * Records that {@code oldId} was merged into {@code newId} within this
	 * model during composition/unification. Enables consumers of an exported
	 * model to reference a merged element by any of its pre-merge ids.
	 */
	public void recordAlias(String oldId, String newId) {
		aliases.put(oldId, newId);
	}

	/**
	 * Records that {@code id} was minted by unification to stand for a group of
	 * merged elements, rather than written in a source file.
	 *
	 * <p>
	 * Unification names a merged group after a counter ({@code unified_0}), so
	 * the id carries no meaning outside the compiler and cannot be traced back
	 * to anything the author wrote. Exporters that address an audience, such as
	 * the Python module a developer implements against, use this to name the
	 * element by the ids it was merged from instead. Contrast the elements a
	 * composition operator synthesises ({@code assembleConclusion}), which are
	 * genuine elements of the composed model carrying labels the author
	 * supplied in the operator call, and are <em>not</em> recorded here.
	 */
	public void recordUnifiedId(String id) {
		unifiedIds.add(id);
	}

	/**
	 * Records the operator call this model was built by. The JPIPE export
	 * writes a composed model back as that call, since its ids, qualified by
	 * the models it was composed from, cannot be declared in a body.
	 */
	public void recordComposition(Composition composition) {
		if (this.composition != null) {
			throw new IllegalStateException(
					"Model '" + name + "' is already composed");
		}
		this.composition = composition;
	}

	/** The operator call this model was built by, if it was composed. */
	public Optional<Composition> composition() {
		return Optional.ofNullable(composition);
	}

	/** Unmodifiable view of the ids unification minted within this model. */
	public Set<String> unifiedIds() {
		return Collections.unmodifiableSet(unifiedIds);
	}

	/**
	 * Unmodifiable view of this model's aliases. Keys are original element ids;
	 * values are the id they were merged into.
	 */
	public Map<String, String> aliases() {
		return Collections.unmodifiableMap(aliases);
	}

	/**
	 * {@code id} followed through this model's aliases to the id it was last
	 * merged into: {@code id} itself when it is no alias.
	 */
	public String resolveAlias(String id) {
		return Aliases.follow(id, aliases::get);
	}

	/**
	 * Rejects {@link Conclusion} — use {@link #setConclusion(Conclusion)}
	 * instead.
	 */
	public void addElement(JustificationElement element) {
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
	 * Every template element is copied, {@link AbstractSupport} placeholders
	 * included, so that override commands can find and replace them in a
	 * {@link Justification} after inlining.
	 *
	 * <p>
	 * The template's conclusion is always copied as a {@link Conclusion},
	 * maintaining its top-level status. If this model already has a conclusion,
	 * the template's conclusion is mapped to it for edge re-wiring but not
	 * added as a second conclusion.
	 *
	 * <p>
	 * The copies come before the elements this model already holds, so that its
	 * elements are in the same order whether its template is declared before or
	 * after it: a composition meets the elements of its sources in that order,
	 * and numbers the elements it unifies accordingly.
	 */
	public void inline(Template template, String templateName) {
		setParent(template);

		// Step 1: build copy map (original plain id → copied element with
		// qualified id)
		Map<String, JustificationElement> copies = new LinkedHashMap<>();

		template.conclusion().ifPresent(tc -> {
			Conclusion copy = (Conclusion) qualifiedCopy(tc, templateName);
			if (this.conclusion == null) {
				this.setConclusion(copy);
			}
			// In Step 2, we want edges supporting 'tc' to now support our
			// conclusion
			copies.put(tc.id(), this.conclusion);
		});

		template.getElements().forEach(elem -> copies.put(elem.id(),
				qualifiedCopy(elem, templateName)));

		// Step 2: re-wire support edges between copies
		rewireEdges(template, copies);

		// Step 3: add copies to this model, before its own elements
		elements.addAll(0, copies.values().stream()
				.filter(c -> !(c instanceof Conclusion)).toList());
	}

	private void rewireEdges(Template template,
			Map<String, JustificationElement> copies) {
		template.conclusion()
				.ifPresent(tc -> rewireConclusionSupport(tc, copies));
		template.subConclusions().forEach(sc -> sc.getSupport().ifPresent(s -> {
			if (copies.containsKey(sc.id()) && copies.containsKey(s.id())) {
				((SubConclusion) copies.get(sc.id()))
						.addSupport((Strategy) copies.get(s.id()));
			}
		}));
		template.strategies().forEach(s -> s.getSupports().forEach(leaf -> {
			if (copies.containsKey(s.id()) && copies.containsKey(leaf.id())) {
				((Strategy) copies.get(s.id()))
						.addSupport((SupportLeaf) copies.get(leaf.id()));
			}
		}));
	}

	private void rewireConclusionSupport(Conclusion tc,
			Map<String, JustificationElement> copies) {
		tc.getSupport().ifPresent(s -> {
			if (!copies.containsKey(tc.id()) || !copies.containsKey(s.id())) {
				return;
			}
			JustificationElement copiedC = copies.get(tc.id());
			Strategy copiedS = (Strategy) copies.get(s.id());
			if (copiedC instanceof Conclusion c) {
				c.addSupport(copiedS);
			} else if (copiedC instanceof SubConclusion sc) {
				sc.addSupport(copiedS);
			}
		});
	}

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

	public List<JustificationElement> getElements() {
		return Collections.unmodifiableList(elements);
	}

	/**
	 * Every element of this model: the conclusion, when one is set, followed by
	 * {@link #getElements()}.
	 */
	public List<JustificationElement> allElements() {
		List<JustificationElement> all = new ArrayList<>(elements.size() + 1);
		conclusion().ifPresent(all::add);
		all.addAll(elements);
		return Collections.unmodifiableList(all);
	}

	/**
	 * Calls {@code action} once per support edge of this model, passing the id
	 * of the supported element and then the id of its supporter. The
	 * conclusion's edge comes first, then those of the sub-conclusions, then
	 * those of the strategies, each in model order.
	 */
	public void forEachSupportEdge(BiConsumer<String, String> action) {
		conclusion().ifPresent(c -> c.getSupport()
				.ifPresent(s -> action.accept(c.id(), s.id())));
		subConclusions().forEach(sc -> sc.getSupport()
				.ifPresent(s -> action.accept(sc.id(), s.id())));
		strategies().forEach(s -> s.getSupports()
				.forEach(leaf -> action.accept(s.id(), leaf.id())));
	}

	/**
	 * The strategies of this model that {@code leafId} supports, in model
	 * order.
	 */
	public List<Strategy> strategiesSupportedBy(String leafId) {
		return strategies().stream().filter(s -> s.getSupports().stream()
				.anyMatch(leaf -> leaf.id().equals(leafId))).toList();
	}

	/**
	 * Returns {@code true} when the conclusion was defined directly in this
	 * model, i.e. it was not inherited from a parent template via
	 * {@link #inline}. Returns {@code false} when there is no conclusion yet or
	 * when the conclusion was set by inlining a parent template.
	 */
	public boolean hasOwnConclusion() {
		if (conclusion == null || parent == null) {
			return conclusion != null;
		}
		return !ancestorTypes().containsKey(conclusion.id());
	}

	/**
	 * Elements defined directly in this model — their id has no counterpart in
	 * any ancestor template.
	 */
	public List<JustificationElement> ownElements() {
		Map<String, Class<? extends JustificationElement>> pt = ancestorTypes();
		return elements.stream().filter(e -> !pt.containsKey(e.id())).toList();
	}

	/**
	 * Elements copied from an ancestor template whose runtime type is unchanged
	 * (not overridden).
	 */
	public List<JustificationElement> inheritedElements() {
		Map<String, Class<? extends JustificationElement>> pt = ancestorTypes();
		return elements.stream().filter(e -> pt.get(e.id()) == e.getClass())
				.toList();
	}

	/**
	 * Elements this model declares rather than inherits: its
	 * {@link #ownElements()} and its {@link #concreteOverrides()}, in model
	 * order.
	 */
	public List<JustificationElement> declaredElements() {
		Map<String, Class<? extends JustificationElement>> pt = ancestorTypes();
		return elements.stream().filter(e -> pt.get(e.id()) != e.getClass())
				.toList();
	}

	/**
	 * Elements that replaced an ancestor's {@link AbstractSupport} placeholder
	 * — same qualified id, but a concrete type (e.g. {@link Evidence} or
	 * {@link SubConclusion}).
	 *
	 * <p>
	 * Together with {@link #ownElements()} and {@link #inheritedElements()},
	 * these three lists partition {@link #getElements()}.
	 */
	public List<JustificationElement> concreteOverrides() {
		Map<String, Class<? extends JustificationElement>> pt = ancestorTypes();
		return elements.stream().filter(
				e -> pt.containsKey(e.id()) && pt.get(e.id()) != e.getClass())
				.toList();
	}

	/**
	 * Returns {@code true} when {@code id} names an ancestor-template element
	 * of any type <em>other than</em> an {@link AbstractSupport} placeholder (a
	 * strategy, conclusion, sub-conclusion, or evidence inherited from the
	 * template) — template-internal structure that a model implementing the
	 * template must not reference. A model may only override the template's
	 * {@code @support} placeholders, never reach inside its structure.
	 *
	 * <p>
	 * Returns {@code false} for template {@code @support} ids (whether
	 * overridden or not) and for ids local to this model. The {@code id} must
	 * be the resolved (qualified) element id.
	 */
	public boolean isInheritedNonSupport(String id) {
		Class<? extends JustificationElement> type = ancestorTypes().get(id);
		return type != null && type != AbstractSupport.class;
	}

	/**
	 * Builds a map from qualified element id to its runtime class for every
	 * element (including the conclusion) in the direct parent template. Because
	 * the parent's element list was already flattened by its own
	 * {@link #inline} call, this map transitively covers all ancestors.
	 *
	 * <p>
	 * Plain ids in the parent (e.g. {@code s}) are qualified to match how
	 * {@link #inline} stored them in this model (e.g. {@code parent:s}).
	 * Already-qualified ids (grandparent elements) are kept as-is.
	 */
	private Map<String, Class<? extends JustificationElement>> ancestorTypes() {
		if (parent == null) {
			return Map.of();
		}
		String parentName = parent.getName();
		Map<String, Class<? extends JustificationElement>> map = new LinkedHashMap<>();
		// Reads the parent's fields directly rather than through allElements(),
		// which copies them: AddSupport calls this on every edge.
		parent.conclusion()
				.ifPresent(c -> map.put(
						QualifiedIds.qualifyIfPlain(parentName, c.id()),
						c.getClass()));
		parent.getElements()
				.forEach(e -> map.put(
						QualifiedIds.qualifyIfPlain(parentName, e.id()),
						e.getClass()));
		return map;
	}

	/**
	 * Searches both the conclusion field and the elements list, so callers do
	 * not need to know how the conclusion is stored.
	 *
	 * <p>
	 * Command conditions call this on every pass of the execution engine, so it
	 * reads the fields directly rather than going through
	 * {@link #allElements()}, which copies the model's elements.
	 */
	public Optional<JustificationElement> findById(String id) {
		if (conclusion != null && conclusion.id().equals(id)) {
			return Optional.of(conclusion);
		}
		Optional<JustificationElement> exact = elements.stream()
				.filter(e -> e.id().equals(id))
				.map(JustificationElement.class::cast).findFirst();
		if (exact.isPresent()) {
			return exact;
		}
		// Short-name suffix fallback: resolves a plain id to an inherited
		// qualified element (e.g. "s" resolves to "t:s" after template
		// expansion). The first match, in the order of inheritedMatches,
		// without listing them all: this is the engine's hot path.
		String suffix = ":" + id;
		Optional<JustificationElement> inherited = elements.stream()
				.filter(e -> e.id().endsWith(suffix))
				.map(JustificationElement.class::cast).findFirst();
		if (inherited.isPresent() || conclusion == null
				|| !conclusion.id().endsWith(suffix)) {
			return inherited;
		}
		return Optional.of(conclusion);
	}

	/**
	 * What {@code id}, as the source wrote it, names in this model: the element
	 * {@link #findById} resolves it to, nothing, or the inherited elements it
	 * names ambiguously. A caller that binds an id the source wrote uses this,
	 * so that several matches are reported rather than settled by order
	 * (ADR-0012, #177); {@link #findById} stays for the engine's hot path.
	 */
	public Binding bind(String id) {
		List<JustificationElement> ambiguous = ambiguousMatches(id);
		if (!ambiguous.isEmpty()) {
			return new Binding.Ambiguous(ambiguous);
		}
		return findById(id).<Binding>map(Binding.Found::new)
				.orElseGet(Binding.Missing::new);
	}

	/**
	 * The elements {@code id} names ambiguously: when no element has that id,
	 * the inherited ones it names through the short-name fallback of
	 * {@link #findById}, if there are several; nothing otherwise.
	 */
	private List<JustificationElement> ambiguousMatches(String id) {
		boolean exact = (conclusion != null && conclusion.id().equals(id))
				|| elements.stream().anyMatch(e -> e.id().equals(id));
		if (exact) {
			return List.of();
		}
		List<JustificationElement> matches = inheritedMatches(id);
		return matches.size() > 1 ? matches : List.of();
	}

	/**
	 * The inherited elements a plain {@code id} names through the short-name
	 * fallback of {@link #findById}: every element, the inherited conclusion
	 * included, whose qualified id ends with {@code :id}. More than one is an
	 * ambiguity the caller must not settle by picking the first (ADR-0012).
	 */
	public List<JustificationElement> inheritedMatches(String id) {
		String suffix = ":" + id;
		List<JustificationElement> matches = new ArrayList<>();
		elements.stream().filter(e -> e.id().endsWith(suffix))
				.forEach(matches::add);
		// Last, so that the conclusion never takes the place of the element
		// the fallback picked before it was considered.
		if (conclusion != null && conclusion.id().endsWith(suffix)) {
			matches.add(conclusion);
		}
		return matches;
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

	/**
	 * Creates a copy of {@code elem}. If the element's id is already qualified
	 * (contains {@code :}), the id is preserved as-is — it was inherited from a
	 * grandparent template and must not be double-qualified. Plain ids are
	 * prefixed with {@code prefix:}.
	 */
	protected static JustificationElement qualifiedCopy(
			JustificationElement elem, String prefix) {
		String qualifiedId = QualifiedIds.qualifyIfPlain(prefix, elem.id());
		return switch (elem) {
			case Evidence e -> new Evidence(qualifiedId, e.label());
			case Strategy s -> new Strategy(qualifiedId, s.label());
			case SubConclusion sc -> new SubConclusion(qualifiedId, sc.label());
			case Conclusion c -> new Conclusion(qualifiedId, c.label());
			case AbstractSupport as ->
				new AbstractSupport(qualifiedId, as.label());
		};
	}
}
