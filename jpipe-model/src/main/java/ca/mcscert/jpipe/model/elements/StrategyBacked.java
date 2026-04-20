package ca.mcscert.jpipe.model.elements;

import java.util.Optional;

/**
 * Sealed interface for elements that receive support from exactly one
 * {@link Strategy}. The symmetric counterpart to {@link SupportLeaf}: where
 * {@code SupportLeaf} marks elements that <em>provide</em> support, this
 * interface marks elements that <em>receive</em> it.
 *
 * <p>
 * Only {@link Conclusion} and {@link SubConclusion} may implement this
 * interface. Note that {@link SubConclusion} implements both — it is a
 * {@code SupportLeaf} (it can support a strategy chain) and a
 * {@code StrategyBacked} (it is itself supported by a strategy).
 */
public sealed interface StrategyBacked permits Conclusion, SubConclusion {

	/**
	 * Registers {@code supporter} as the single strategy backing this element.
	 */
	void addSupport(Strategy supporter);

	/**
	 * Returns the strategy backing this element, if one has been registered.
	 */
	Optional<Strategy> getSupport();
}
