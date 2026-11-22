package ca.mcscert.jpipe.commands.creation;

import ca.mcscert.jpipe.util.QualifiedIds;
import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import java.util.Optional;

/**
 * Abstract base for all element-creation commands. Holds the four fields shared
 * by every creation command ({@code container}, {@code identifier},
 * {@code label}, {@code location}) and implements everything that does not
 * depend on the element's kind, so each concrete command only says which
 * element it creates.
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
	@Override
	public Optional<String> target() {
		return Optional.of(container);
	}

	@Override
	public ElementCreationCommand withId(String newId) {
		return ElementCreationCommand.of(container, newId, element(), location);
	}

	/**
	 * Adds the element to its container and records where it was declared.
	 * {@link CreateConclusion} overrides it: a model holds its conclusion apart
	 * from its other elements.
	 */
	@Override
	public void doExecute(Unit context) {
		context.addInto(container, element());
		context.recordLocation(container, identifier, location);
	}

	/** {@code create_<kind>('container', 'id', 'label').} */
	@Override
	public String toString() {
		return "create_" + element().kind().replace('-', '_') + "('" + container
				+ "', '" + identifier + "', '" + label + "').";
	}

	@Override
	public ElementCreationCommand withNamespace(String ns) {
		return ElementCreationCommand.of(QualifiedIds.qualify(ns, container),
				QualifiedIds.inNamespace(ns, identifier), element(), location);
	}
}
