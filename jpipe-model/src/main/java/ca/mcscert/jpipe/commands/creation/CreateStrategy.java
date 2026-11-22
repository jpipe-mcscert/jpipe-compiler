package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.model.SourceLocation;
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
	public Strategy element() {
		return new Strategy(identifier, label);
	}
}
