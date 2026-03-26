package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Strategy;

/** Creates a {@link Strategy} inside a justification. */
public final class CreateStrategy extends RegularCommand {

	private final String container;
	private final String identifier;
	private final String label;

	public CreateStrategy(String container, String identifier, String label) {
		this.container = container;
		this.identifier = identifier;
		this.label = label;
	}

	@Override
	public void execute(Unit context) {
		context.addInto(container, new Strategy(identifier, label));
	}

	@Override
	public String toString() {
		return "CreateStrategy{container='" + container + "', identifier='" + identifier + "', label='" + label + "'}";
	}
}
