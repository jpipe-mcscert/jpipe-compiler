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

	public ImplementsTemplate(String modelName, String templateName) {
		this.modelName = modelName;
		this.templateName = templateName;
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
			context.recordLocation(modelName, templateName + ":" + tc.id(),
					loc);
		});
		for (JustificationElement elem : template.getElements()) {
			SourceLocation loc = context.locationOf(templateName, elem.id());
			context.recordLocation(modelName, templateName + ":" + elem.id(),
					loc);
		}
	}

	@Override
	public String toString() {
		return "implements('" + modelName + "', '" + templateName + "').";
	}
}
