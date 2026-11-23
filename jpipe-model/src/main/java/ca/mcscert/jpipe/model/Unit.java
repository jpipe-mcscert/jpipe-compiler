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
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Root of a compiled jPipe file. Contains all named justification models
 * defined in the file.
 */
public final class Unit {

	private final String source;
	private final Map<String, JustificationModel> models = new LinkedHashMap<>();
	private final Map<String, SourceLocation> locations = new LinkedHashMap<>();
	private final Set<String> incomplete = new LinkedHashSet<>();
	private final Set<String> failedOverrides = new LinkedHashSet<>();

	public Unit(String source) {
		this.source = source;
	}

	public String getSource() {
		return source;
	}

	public void add(JustificationModel model) {
		if (models.containsKey(model.getName())) {
			throw new IllegalStateException(
					"Duplicate model name: '" + model.getName() + "'");
		}
		models.put(model.getName(), model);
	}

	/**
	 * Records that {@code modelName} could not be built in full: its
	 * {@code implements} failed or never ran. Nothing should consume it, and
	 * its gaps are no separate mistakes: the cause is reported where it is.
	 */
	public void markIncomplete(String modelName) {
		incomplete.add(modelName);
	}

	/** Whether {@code modelName} was built in full; see markIncomplete. */
	public boolean isComplete(String modelName) {
		return !incomplete.contains(modelName);
	}

	/**
	 * Records that an override of {@code elementId} in {@code modelName}
	 * failed. The placeholder it meant to replace is left in the model, and is
	 * no second mistake: the failure is reported where it is.
	 */
	public void recordFailedOverride(String modelName, String elementId) {
		failedOverrides.add(registryKey(modelName, elementId));
	}

	/** Whether an override of {@code elementId} in {@code modelName} failed. */
	public boolean overrideFailed(String modelName, String elementId) {
		return failedOverrides.contains(registryKey(modelName, elementId));
	}

	public Collection<JustificationModel> getModels() {
		return Collections.unmodifiableCollection(models.values());
	}

	public Optional<JustificationModel> findModel(String name) {
		return Optional.ofNullable(models.get(name));
	}

	@SuppressWarnings("java:S1452")
	public JustificationModel get(String name) {
		return findModel(name).orElseThrow(
				() -> new NoSuchElementException("Unknown model: " + name));
	}

	public void addInto(String modelName, JustificationElement element) {
		switch (get(modelName)) {
			case Justification j -> {
				// A justification declares common elements only; placeholders
				// reach it through template expansion, not through here.
				if (!(element instanceof CommonElement)) {
					throw new IllegalArgumentException("Justification '"
							+ modelName + "' only accepts CommonElement; got "
							+ element.getClass().getSimpleName());
				}
				j.addElement(element);
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
			locations.put(registryKey(modelName, elementId), loc);
		}
	}

	/**
	 * Returns the declared location of an element inside a model, or
	 * {@link SourceLocation#UNKNOWN}.
	 */
	public SourceLocation locationOf(String modelName, String elementId) {
		return locations.getOrDefault(registryKey(modelName, elementId),
				SourceLocation.UNKNOWN);
	}

	/**
	 * Records that {@code oldId} in model {@code modelName} was merged into
	 * {@code newId}. See {@link JustificationModel#recordAlias}.
	 *
	 * @throws NoSuchElementException
	 *             if {@code modelName} does not exist yet: an alias is recorded
	 *             after the model it belongs to is created.
	 */
	public void recordAlias(String modelName, String oldId, String newId) {
		get(modelName).recordAlias(oldId, newId);
	}

	/**
	 * Records that {@code id} in model {@code modelName} was minted by
	 * unification. See {@link JustificationModel#recordUnifiedId(String)}.
	 */
	public void recordUnifiedId(String modelName, String id) {
		findModel(modelName).ifPresent(m -> m.recordUnifiedId(id));
	}

	public <R> R accept(JustificationVisitor<R> visitor) {
		return visitor.visit(this);
	}

	/**
	 * Registry key of an element inside a model: {@code "modelName/elementId"}.
	 */
	public static String registryKey(String modelName, String elementId) {
		return modelName + "/" + elementId;
	}
}
