package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Links a justification model to the {@link Template} it implements, by setting
 * the template as the model's parent.
 *
 * <p>
 * The command defers execution until both models are present in the unit.
 */
public final class ImplementsTemplate extends RegularCommand {

	private final String modelName;
	private final String templateName;
	private final SourceLocation location;

	public ImplementsTemplate(String modelName, String templateName) {
		this(modelName, templateName, SourceLocation.UNKNOWN);
	}

	public ImplementsTemplate(String modelName, String templateName,
			SourceLocation location) {
		this.modelName = modelName;
		this.templateName = templateName;
		this.location = location;
	}

	public String modelName() {
		return modelName;
	}

	public String templateName() {
		return templateName;
	}

	public SourceLocation location() {
		return location;
	}

	@Override
	public Predicate<Unit> condition() {
		return unit -> unit.findModel(modelName).isPresent()
				&& unit.findModel(templateName).isPresent();
	}

	@Override
	public void doExecute(Unit context) {
		Template template = context.findModel(templateName)
				.filter(Template.class::isInstance).map(Template.class::cast)
				.orElseThrow(() -> new NoSuchElementException(
						"No template named: " + templateName));
		context.get(modelName).inline(template, templateName);
		propagateLocations(context, template);
	}

	/**
	 * Copies source locations of template elements into the justification's
	 * registry under their qualified ids (e.g. {@code templateName:elementId}).
	 * This lets downstream checkers report the original declaration site even
	 * for elements that were inherited rather than declared directly.
	 */
	private void propagateLocations(Unit context, Template template) {
		template.conclusion().ifPresent(tc -> {
			SourceLocation loc = context.locationOf(templateName, tc.id());
			String key = tc.id().contains(":")
					? tc.id()
					: templateName + ":" + tc.id();
			context.recordLocation(modelName, key, loc);
		});
		for (JustificationElement elem : template.getElements()) {
			SourceLocation loc = context.locationOf(templateName, elem.id());
			String key = elem.id().contains(":")
					? elem.id()
					: templateName + ":" + elem.id();
			context.recordLocation(modelName, key, loc);
		}
	}

	@Override
	public String toString() {
		return "implements('" + modelName + "', '" + templateName + "').";
	}
}
