package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.List;

/**
 * What an id the source wrote names in a model: one element, none, or several
 * inherited ones it cannot choose between (ADR-0012, #177). Returned by
 * {@link JustificationModel#bind}, so that a caller binding an id cannot take
 * the first of several matches for the one it names.
 */
public sealed interface Binding {

	/** The id names exactly {@code element}. */
	record Found(JustificationElement element) implements Binding {
	}

	/** The id names no element of the model. */
	record Missing() implements Binding {
	}

	/**
	 * The id names no element of its own, and several inherited ones through
	 * the short-name fallback: {@code candidates}, in model order.
	 */
	record Ambiguous(List<JustificationElement> candidates) implements Binding {

		public Ambiguous {
			candidates = List.copyOf(candidates);
		}
	}
}
