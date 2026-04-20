package ca.mcscert.jpipe.model.elements;

/** The top-level conclusion that a justification aims to establish. */
public final class Conclusion extends AbstractSupportedElement
		implements
			CommonElement,
			StrategyBacked {

	public Conclusion(String id, String label) {
		super(id, label);
	}
}
