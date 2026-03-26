package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;

/** Creates a {@link Template} inside a unit. */
public final class CreateTemplate extends RegularCommand {

	private final String identifier;

	public CreateTemplate(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public void execute(Unit context) {
		context.add(new Template(identifier));
	}

	@Override
	public String toString() {
		return "CreateTemplate{identifier='" + identifier + "'}";
	}
}
