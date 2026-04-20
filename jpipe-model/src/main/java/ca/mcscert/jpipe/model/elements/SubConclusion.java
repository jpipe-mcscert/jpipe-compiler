package ca.mcscert.jpipe.model.elements;

/** An intermediate conclusion within a justification. */
public final class SubConclusion extends AbstractSupportedElement
		implements
			CommonElement,
			StrategyBacked,
			SupportLeaf {

	public SubConclusion(String id, String label) {
		super(id, label);
	}
}
