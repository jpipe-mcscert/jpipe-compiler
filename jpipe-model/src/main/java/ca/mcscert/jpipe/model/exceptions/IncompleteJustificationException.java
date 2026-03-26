package ca.mcscert.jpipe.model.exceptions;

import ca.mcscert.jpipe.model.Justification;
import java.util.List;

/**
 * Thrown when {@link Justification#lock()} detects that one or more elements
 * have no supporter assigned.
 */
public class IncompleteJustificationException extends IllegalStateException {

	private final String justificationName;
	private final List<String> incompleteElementIds;

	public IncompleteJustificationException(String justificationName, List<String> incompleteElementIds) {
		super("Justification '" + justificationName + "' is incomplete: unsupported elements " + incompleteElementIds);
		this.justificationName = justificationName;
		this.incompleteElementIds = List.copyOf(incompleteElementIds);
	}

	public String getJustificationName() {
		return justificationName;
	}

	public List<String> getIncompleteElementIds() {
		return incompleteElementIds;
	}
}
