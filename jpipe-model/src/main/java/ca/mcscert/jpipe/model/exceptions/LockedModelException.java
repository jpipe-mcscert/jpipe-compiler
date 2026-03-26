package ca.mcscert.jpipe.model.exceptions;

import ca.mcscert.jpipe.model.JustificationModel;

/**
 * Thrown when an attempt is made to modify a {@link JustificationModel} after
 * it has been locked via {@link JustificationModel#lock()}.
 */
public class LockedModelException extends IllegalStateException {

	public LockedModelException(String modelName) {
		super("Model '" + modelName + "' is locked and cannot be modified");
	}
}
