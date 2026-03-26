package ca.mcscert.jpipe.model;

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

	public Unit(String source) {
		this.source = source;
	}

	public String getSource() {
		return source;
	}

	public void add(JustificationModel<?> model) {
		models.put(model.getName(), model);
	}

	public Collection<JustificationModel<?>> getModels() {
		return Collections.unmodifiableCollection(models.values());
	}

	public Optional<JustificationModel<?>> findModel(String name) {
		return Optional.ofNullable(models.get(name));
	}

	public JustificationModel<?> get(String name) {
		return findModel(name).orElseThrow(() -> new NoSuchElementException("Unknown model: " + name));
	}

	@SuppressWarnings("unchecked")
	public void addInto(String modelName, JustificationElement element) {
		((JustificationModel<JustificationElement>) get(modelName)).addElement(element);
	}

	public List<Justification> justifications() {
		return models.values().stream().filter(Justification.class::isInstance).map(Justification.class::cast).toList();
	}

	public List<Template> templates() {
		return models.values().stream().filter(Template.class::isInstance).map(Template.class::cast).toList();
	}

	public <R> R accept(JustificationVisitor<R> visitor) {
		R result = visitor.visit(this);
		models.values().forEach(m -> m.accept(visitor));
		return result;
	}
}
