package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Strategy;

/** Creates a {@link Strategy} inside a justification. */
public final class CreateStrategy extends AbstractElementCreationCommand {

	public CreateStrategy(String container, String identifier, String label) {
		this(container, identifier, label, SourceLocation.UNKNOWN);
	}

	public CreateStrategy(String container, String identifier, String label,
			SourceLocation location) {
		super(container, identifier, label, location);
	}

	@Override
	public ElementCreationCommand withId(String newId) {
		return new CreateStrategy(container, newId, label, location);
	}

	@Override
	public void doExecute(Unit context) {
		context.addInto(container, new Strategy(identifier, label));
		context.recordLocation(container, identifier, location);
	}

	@Override
	public String toString() {
		return "create_strategy('" + container + "', '" + identifier + "', '"
				+ label + "').";
	}
}
