package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Unit;
import java.util.function.Predicate;

/** Removes an element by id from a justification model. */
public final class RemoveElement extends RegularCommand {

	private final String container;
	private final String elementId;

	public RemoveElement(String container, String elementId) {
		this.container = container;
		this.elementId = elementId;
	}

	@Override
	public Predicate<Unit> condition() {
		return unit -> unit.findModel(container).map(m -> m.findById(elementId).isPresent()).orElse(false);
	}

	@Override
	protected void doExecute(Unit context) {
		context.removeFrom(container, elementId);
	}

	@Override
	public String toString() {
		return "remove('" + container + "', '" + elementId + "')";
	}
}
