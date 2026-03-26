package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
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
		return unit -> unit.findModel(modelName).isPresent() && unit.findModel(templateName).isPresent();
	}

	@Override
	public void doExecute(Unit context) {
		Template template = context.findModel(templateName).filter(Template.class::isInstance).map(Template.class::cast)
				.orElseThrow(() -> new NoSuchElementException("No template named: " + templateName));
		context.get(modelName).setParent(template);
	}

	@Override
	public String toString() {
		return "implements('" + modelName + "', '" + templateName + "').";
	}
}
