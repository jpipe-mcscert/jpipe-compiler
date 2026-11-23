package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.elements.Evidence;

/** Creates an {@link Evidence} inside a justification. */
public final class CreateEvidence extends AbstractElementCreationCommand {

	public CreateEvidence(String container, String identifier, String label) {
		this(container, identifier, label, SourceLocation.UNKNOWN);
	}

	public CreateEvidence(String container, String identifier, String label,
			SourceLocation location) {
		super(container, identifier, label, location);
	}

	@Override
	public Evidence element() {
		return new Evidence(identifier, label);
	}
}
