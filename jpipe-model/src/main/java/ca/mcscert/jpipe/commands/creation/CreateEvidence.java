package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Evidence;

/** Creates an {@link Evidence} inside a justification. */
public final class CreateEvidence extends RegularCommand {

	private final String container;
	private final String identifier;
	private final String label;
	private final SourceLocation location;

	public CreateEvidence(String container, String identifier, String label) {
		this(container, identifier, label, SourceLocation.UNKNOWN);
	}

	public CreateEvidence(String container, String identifier, String label,
			SourceLocation location) {
		this.container = container;
		this.identifier = identifier;
		this.label = label;
		this.location = location;
	}

	public String container() {
		return container;
	}

	public String identifier() {
		return identifier;
	}

	public String label() {
		return label;
	}

	public SourceLocation location() {
		return location;
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
