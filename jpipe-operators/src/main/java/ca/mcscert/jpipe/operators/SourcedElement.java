package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.elements.JustificationElement;

/**
 * Pairs a {@link JustificationElement} with the model it was taken from and its
 * original source location, so merge functions can inspect the element in its
 * original context and forward the location to creation commands.
 */
public record SourcedElement(JustificationElement element,
		JustificationModel<?> source, SourceLocation location) {
}
