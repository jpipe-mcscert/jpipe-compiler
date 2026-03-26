package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;

/** Creates a {@link Conclusion} inside a justification or template. */
public final class CreateConclusion extends RegularCommand {

	private final String container;
	private final String identifier;
	private final String label;

	public CreateConclusion(String container, String identifier, String label) {
		this.container = container;
		this.identifier = identifier;
		this.label = label;
	}

	@Override
	public void execute(Unit context) {
		context.get(container).setConclusion(new Conclusion(identifier, label));
	}

	@Override
	public String toString() {
		return "CreateConclusion{container='" + container + "', identifier='" + identifier + "', label='" + label
				+ "'}";
	}
}
