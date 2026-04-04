package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;

/** Creates a {@link Conclusion} inside a justification or template. */
public final class CreateConclusion extends RegularCommand {

	private final String container;
	private final String identifier;
	private final String label;
	private final SourceLocation location;

	public CreateConclusion(String container, String identifier, String label) {
		this(container, identifier, label, SourceLocation.UNKNOWN);
	}

	public CreateConclusion(String container, String identifier, String label,
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
		context.get(container).setConclusion(new Conclusion(identifier, label));
		context.recordLocation(container, identifier, location);
	}

	@Override
	public String toString() {
		return "create_conclusion('" + container + "', '" + identifier + "', '"
				+ label + "').";
	}
}
