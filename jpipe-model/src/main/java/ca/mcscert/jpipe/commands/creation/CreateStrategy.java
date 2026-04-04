package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Strategy;

/** Creates a {@link Strategy} inside a justification. */
public final class CreateStrategy extends RegularCommand {

	private final String container;
	private final String identifier;
	private final String label;
	private final SourceLocation location;

	public CreateStrategy(String container, String identifier, String label) {
		this(container, identifier, label, SourceLocation.UNKNOWN);
	}

	public CreateStrategy(String container, String identifier, String label,
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
		context.addInto(container, new Strategy(identifier, label));
		context.recordLocation(container, identifier, location);
	}

	@Override
	public String toString() {
		return "create_strategy('" + container + "', '" + identifier + "', '"
				+ label + "').";
	}
}
