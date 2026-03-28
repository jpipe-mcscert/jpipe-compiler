package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;

/** Creates an {@link AbstractSupport} inside a template. */
public final class CreateAbstractSupport extends RegularCommand {

	private final String container;
	private final String identifier;
	private final String label;
	private final SourceLocation location;

	public CreateAbstractSupport(String container, String identifier,
			String label) {
		this(container, identifier, label, SourceLocation.UNKNOWN);
	}

	public CreateAbstractSupport(String container, String identifier,
			String label, SourceLocation location) {
		this.container = container;
		this.identifier = identifier;
		this.label = label;
		this.location = location;
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
