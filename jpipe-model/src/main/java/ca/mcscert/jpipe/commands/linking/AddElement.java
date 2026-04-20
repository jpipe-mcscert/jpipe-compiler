package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.function.Predicate;

/** Adds a pre-built element to a justification model. */
public final class AddElement extends RegularCommand {

	private final String container;
	private final JustificationElement element;

	public AddElement(String container, JustificationElement element) {
		this.container = container;
		this.element = element;
	}

	@Override
	public Predicate<Unit> condition() {
		return unit -> unit.findModel(container).isPresent();
	}

	@Override
	protected void doExecute(Unit context) {
		context.addInto(container, element);
	}

	@Override
	public String toString() {
		return "add('" + container + "', " + element + ")";
	}
}
