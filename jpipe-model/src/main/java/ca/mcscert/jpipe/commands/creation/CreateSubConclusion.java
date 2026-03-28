package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.SubConclusion;

/** Creates a {@link SubConclusion} inside a justification. */
public final class CreateSubConclusion extends RegularCommand {

	private final String container;
	private final String identifier;
	private final String label;
	private final SourceLocation location;

	public CreateSubConclusion(String container, String identifier,
			String label) {
		this(container, identifier, label, SourceLocation.UNKNOWN);
	}

	public CreateSubConclusion(String container, String identifier,
			String label, SourceLocation location) {
		this.container = container;
		this.identifier = identifier;
		this.label = label;
		this.location = location;
	}

	@Override
	public void doExecute(Unit context) {
		context.addInto(container, new SubConclusion(identifier, label));
		context.recordLocation(container, identifier, location);
	}

	@Override
	public String toString() {
		return "create_sub_conclusion('" + container + "', '" + identifier
				+ "', '" + label + "').";
	}
}
