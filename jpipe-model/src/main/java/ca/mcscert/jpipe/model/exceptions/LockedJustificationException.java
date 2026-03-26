package ca.mcscert.jpipe.model.exceptions;

import ca.mcscert.jpipe.model.Justification;

/**
 * Thrown when an attempt is made to modify a {@link Justification} after it has
 * been locked via {@link Justification#lock()}.
 */
public class LockedJustificationException extends IllegalStateException {

	public LockedJustificationException(String justificationName) {
		super("Justification '" + justificationName + "' is locked and cannot be modified");
	}
}
