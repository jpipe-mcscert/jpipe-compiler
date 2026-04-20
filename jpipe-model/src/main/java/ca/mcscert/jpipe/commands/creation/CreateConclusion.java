package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;

/** Creates a {@link Conclusion} inside a justification or template. */
public final class CreateConclusion extends AbstractElementCreationCommand {

	public CreateConclusion(String container, String identifier, String label) {
		this(container, identifier, label, SourceLocation.UNKNOWN);
	}

	public CreateConclusion(String container, String identifier, String label,
			SourceLocation location) {
		super(container, identifier, label, location);
	}

	@Override
	public ElementCreationCommand withId(String newId) {
		return new CreateConclusion(container, newId, label, location);
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
