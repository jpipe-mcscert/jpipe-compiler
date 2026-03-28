package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Evidence;

/** Creates an {@link Evidence} inside a justification. */
public final class CreateEvidence extends RegularCommand {

	private final String container;
	private final String identifier;
	private final String label;

	public CreateEvidence(String container, String identifier, String label) {
		this.container = container;
		this.identifier = identifier;
		this.label = label;
	}

	@Override
	public void doExecute(Unit context) {
		context.addInto(container, new Evidence(identifier, label));
	}

	@Override
	public String toString() {
		return "create_evidence('" + container + "', '" + identifier + "', '"
				+ label + "').";
	}
}
