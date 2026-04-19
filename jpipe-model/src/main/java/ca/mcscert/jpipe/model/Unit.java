package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.CommonElement;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.visitor.JustificationVisitor;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Root of a compiled jPipe file. Contains all named justification models
 * defined in the file.
 */
public final class Unit {

	private final String source;
	private final Map<String, JustificationModel<?>> models = new LinkedHashMap<>();
	private final Map<String, SourceLocation> locations = new LinkedHashMap<>();
	private final Map<String, String> aliases = new LinkedHashMap<>();

	public Unit(String source) {
		this.source = source;
	}

	public String getSource() {
		return source;
	}

	public void add(JustificationModel<?> model) {
		if (models.containsKey(model.getName())) {
			throw new IllegalStateException(
					"Duplicate model name: '" + model.getName() + "'");
		}
		models.put(model.getName(), model);
	}

	public Collection<JustificationModel<?>> getModels() {
		return Collections.unmodifiableCollection(models.values());
	}

	public Optional<JustificationModel<?>> findModel(String name) {
		return Optional.ofNullable(models.get(name));
	}

	@SuppressWarnings("java:S1452")
	public JustificationModel<?> get(String name) {
		return findModel(name).orElseThrow(
				() -> new NoSuchElementException("Unknown model: " + name));
	}

	public void addInto(String modelName, JustificationElement element) {
		switch (get(modelName)) {
			case Justification j -> {
				if (!(element instanceof CommonElement ce)) {
					throw new IllegalArgumentException("Justification '"
							+ modelName + "' only accepts CommonElement; got "
							+ element.getClass().getSimpleName());
				}
				j.addElement(ce);
			}
			case Template t -> t.addElement(element);
		}
	}

	public void removeFrom(String modelName, String elementId) {
		get(modelName).removeElement(elementId);
	}

	public List<Justification> justifications() {
		return models.values().stream().filter(Justification.class::isInstance)
				.map(Justification.class::cast).toList();
	}

	public List<Template> templates() {
		return models.values().stream().filter(Template.class::isInstance)
				.map(Template.class::cast).toList();
	}

	/** Unmodifiable view of the full location registry. */
	public Map<String, SourceLocation> locations() {
		return Collections.unmodifiableMap(locations);
	}

	/**
	 * Unmodifiable view of all recorded aliases. Keys are
	 * {@code "modelName/oldId"}; values are the new (merged) id.
	 */
	public Map<String, String> aliases() {
		return Collections.unmodifiableMap(aliases);
	}

	/** Records where a model (justification or template) was declared. */
	public void recordLocation(String modelName, SourceLocation loc) {
		if (loc.isKnown()) {
			locations.put(modelName, loc);
		}
	}

	/**
	 * Returns the declared location of a model, or
	 * {@link SourceLocation#UNKNOWN}.
	 */
	public SourceLocation locationOf(String modelName) {
		return locations.getOrDefault(modelName, SourceLocation.UNKNOWN);
	}

	/** Records where a named element inside a model was declared. */
	public void recordLocation(String modelName, String elementId,
			SourceLocation loc) {
		if (loc.isKnown()) {
			locations.put(modelName + "/" + elementId, loc);
		}
	}

	/**
	 * Returns the declared location of an element inside a model, or
	 * {@link SourceLocation#UNKNOWN}.
	 */
	public SourceLocation locationOf(String modelName, String elementId) {
		return locations.getOrDefault(modelName + "/" + elementId,
				SourceLocation.UNKNOWN);
	}

	/**
	 * Records that {@code oldId} in model {@code modelName} was merged into
	 * {@code newId}.
	 */
	public void recordAlias(String modelName, String oldId, String newId) {
		aliases.put(modelName + "/" + oldId, newId);
	}

	/**
	 * Returns the id that {@code id} resolves to in {@code modelName}, or
	 * {@code id} itself if no alias was registered.
	 */
	public String resolveAlias(String modelName, String id) {
		return aliases.getOrDefault(modelName + "/" + id, id);
	}

	public <R> R accept(JustificationVisitor<R> visitor) {
		return visitor.visit(this);
	}
}
