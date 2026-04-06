package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;

/**
 * Records that {@code oldId} inside a result model is an alias for the merged
 * element {@code newId}. Persisted to {@link Unit} so that post-composition
 * symbol lookups can resolve original element names through
 * {@link Unit#resolveAlias}.
 */
public final class RegisterAlias extends RegularCommand {

	private final String container;
	private final String oldId;
	private final String newId;
	private final SourceLocation location;

	public RegisterAlias(String container, String oldId, String newId) {
		this(container, oldId, newId, SourceLocation.UNKNOWN);
	}

	public RegisterAlias(String container, String oldId, String newId,
			SourceLocation location) {
		this.container = container;
		this.oldId = oldId;
		this.newId = newId;
		this.location = location;
	}

	public String container() {
		return container;
	}

	public String oldId() {
		return oldId;
	}

	public String newId() {
		return newId;
	}

	public SourceLocation location() {
		return location;
	}

	@Override
	public void doExecute(Unit context) {
		context.recordAlias(container, oldId, newId);
	}

	@Override
	public String toString() {
		return "registerAlias('" + container + "', '" + oldId + "' -> '" + newId
				+ "').";
	}
}
