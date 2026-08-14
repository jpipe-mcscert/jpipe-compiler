package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Unit;

/**
 * Records that {@code id} inside a result model was minted by unification to
 * stand for a group of merged elements. Persisted to {@link Unit} so that
 * exporters can tell an id the compiler invented from one an author wrote; see
 * {@link ca.mcscert.jpipe.model.JustificationModel#recordUnifiedId(String)}.
 */
public final class MarkUnified extends RegularCommand {

	private final String container;
	private final String id;

	public MarkUnified(String container, String id) {
		this.container = container;
		this.id = id;
	}

	public String container() {
		return container;
	}

	public String id() {
		return id;
	}

	@Override
	public void doExecute(Unit context) {
		context.recordUnifiedId(container, id);
	}

	@Override
	public String toString() {
		return "markUnified('" + container + "', '" + id + "').";
	}
}
