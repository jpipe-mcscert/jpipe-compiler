package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.JustificationElement;

/**
 * Pairs a {@link JustificationElement} with the model it was taken from, so
 * merge functions can inspect the element in its original context without
 * performing a {@link ca.mcscert.jpipe.model.Unit} lookup.
 */
public record SourcedElement(JustificationElement element,
		JustificationModel<?> source) {
}
