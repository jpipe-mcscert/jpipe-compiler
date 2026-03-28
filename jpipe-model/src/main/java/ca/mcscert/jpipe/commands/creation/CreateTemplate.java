package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;

/** Creates a {@link Template} inside a unit. */
public final class CreateTemplate extends RegularCommand {

	private final String identifier;
	private final SourceLocation location;

	public CreateTemplate(String identifier) {
		this(identifier, SourceLocation.UNKNOWN);
	}

	public CreateTemplate(String identifier, SourceLocation location) {
		this.identifier = identifier;
		this.location = location;
	}

	@Override
	public void doExecute(Unit context) {
		context.add(new Template(identifier));
		context.recordLocation(identifier, location);
	}

	@Override
	public String toString() {
		return "create_template('" + identifier + "').";
	}
}
