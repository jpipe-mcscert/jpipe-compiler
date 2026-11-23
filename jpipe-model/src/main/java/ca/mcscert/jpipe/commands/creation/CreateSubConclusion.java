package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.elements.SubConclusion;

/** Creates a {@link SubConclusion} inside a justification. */
public final class CreateSubConclusion extends AbstractElementCreationCommand {

	public CreateSubConclusion(String container, String identifier,
			String label) {
		this(container, identifier, label, SourceLocation.UNKNOWN);
	}

	public CreateSubConclusion(String container, String identifier,
			String label, SourceLocation location) {
		super(container, identifier, label, location);
	}

	@Override
	public SubConclusion element() {
		return new SubConclusion(identifier, label);
	}
}
