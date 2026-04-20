package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
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
	public ElementCreationCommand withId(String newId) {
		return new CreateEvidence(container, newId, label, location);
	}

	@Override
	public void doExecute(Unit context) {
		context.addInto(container, new Evidence(identifier, label));
		context.recordLocation(container, identifier, location);
	}

	@Override
	public String toString() {
		return "create_evidence('" + container + "', '" + identifier + "', '"
				+ label + "').";
	}
}
