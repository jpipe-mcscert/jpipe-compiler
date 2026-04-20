package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;

/** Creates an {@link AbstractSupport} inside a template. */
public final class CreateAbstractSupport
		extends
			AbstractElementCreationCommand {

	public CreateAbstractSupport(String container, String identifier,
			String label) {
		this(container, identifier, label, SourceLocation.UNKNOWN);
	}

	public CreateAbstractSupport(String container, String identifier,
			String label, SourceLocation location) {
		super(container, identifier, label, location);
	}

	@Override
	public ElementCreationCommand withId(String newId) {
		return new CreateAbstractSupport(container, newId, label, location);
	}

	@Override
	public void doExecute(Unit context) {
		context.addInto(container, new AbstractSupport(identifier, label));
		context.recordLocation(container, identifier, location);
	}

	@Override
	public String toString() {
		return "create_abstract_support('" + container + "', '" + identifier
				+ "', '" + label + "').";
	}
}
