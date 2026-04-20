package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;

/**
 * Abstract base for all element-creation commands. Holds the four fields shared
 * by every creation command ({@code container}, {@code identifier},
 * {@code label}, {@code location}) and provides their accessor implementations,
 * eliminating the structural duplication across the five concrete commands.
 *
 * <p>
 * This class is a package-private implementation detail. The sealed contract is
 * declared on {@link ElementCreationCommand}; this class is its sole direct
 * implementor, with the five concrete commands as its permitted subclasses.
 */
abstract sealed class AbstractElementCreationCommand extends RegularCommand
		implements
			ElementCreationCommand
		permits CreateConclusion, CreateStrategy, CreateEvidence,
		CreateSubConclusion, CreateAbstractSupport {

	protected final String container;
	protected final String identifier;
	protected final String label;
	protected final SourceLocation location;

	protected AbstractElementCreationCommand(String container,
			String identifier, String label, SourceLocation location) {
		this.container = container;
		this.identifier = identifier;
		this.label = label;
		this.location = location;
	}

	@Override
	public String container() {
		return container;
	}

	@Override
	public String identifier() {
		return identifier;
	}

	@Override
	public String label() {
		return label;
	}

	@Override
	public SourceLocation location() {
		return location;
	}
}
