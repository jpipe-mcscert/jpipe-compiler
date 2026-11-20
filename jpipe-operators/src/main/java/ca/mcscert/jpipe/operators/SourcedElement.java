package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.util.QualifiedIds;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.elements.ElementView;
import ca.mcscert.jpipe.model.elements.JustificationElement;

/**
 * Pairs a {@link JustificationElement} with the model it was taken from and its
 * original source location, so merge functions can inspect the element in its
 * original context and forward the location to creation commands.
 *
 * <p>
 * Implements {@link ElementView} so that {@link EquivalenceRelation}
 * implementations can receive {@code SourcedElement}s directly without needing
 * a separate adapter.
 */
public record SourcedElement(JustificationElement element,
		JustificationModel source,
		SourceLocation location) implements ElementView {

	@Override
	public String id() {
		return element().id();
	}

	@Override
	public String label() {
		return element().label();
	}

	/**
	 * The element's id qualified with the name of its source model
	 * ({@code source:id}), the form a composed model gives it by default.
	 */
	public String qualifiedId() {
		return QualifiedIds.qualify(source.getName(), element.id());
	}
}
