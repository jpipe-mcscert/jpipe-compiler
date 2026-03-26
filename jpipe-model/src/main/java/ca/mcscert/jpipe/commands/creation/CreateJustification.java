package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;

/** Creates a {@link Justification} inside a unit. */
public final class CreateJustification extends RegularCommand {

	private final String identifier;

	public CreateJustification(String identifier) {
		this.identifier = identifier;
	}

	@Override
	public void execute(Unit context) {
		context.add(new Justification(identifier));
	}

	@Override
	public String toString() {
		return "CreateJustification{identifier='" + identifier + "'}";
	}
}
