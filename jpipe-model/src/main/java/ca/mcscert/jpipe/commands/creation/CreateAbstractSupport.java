package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;

/** Creates an {@link AbstractSupport} inside a template. */
public final class CreateAbstractSupport extends RegularCommand {

	private final String container;
	private final String identifier;
	private final String label;

	public CreateAbstractSupport(String container, String identifier, String label) {
		this.container = container;
		this.identifier = identifier;
		this.label = label;
	}

	@Override
	public void execute(Unit context) {
		context.addInto(container, new AbstractSupport(identifier, label));
	}

	@Override
	public String toString() {
		return "CreateAbstractSupport{container='" + container + "', identifier='" + identifier + "', label='" + label
				+ "'}";
	}
}
