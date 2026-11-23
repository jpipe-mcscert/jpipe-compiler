package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.Optional;

/**
 * Where an id of a model was copied from: {@code id}, an id of {@code model},
 * which is the template the copy's model implements, or a source of the
 * operator call that built it. Recorded when the copy is made (#209), so that
 * what a model inherits, and from where, is looked up rather than read back
 * from its qualified id.
 *
 * @param model
 *            the model the id was copied from.
 * @param id
 *            the id there: of one of its elements, or of an alias it holds, an
 *            id merged into another.
 */
public record Origin(JustificationModel model, String id) {

	/** The element copied, unless {@code id} is an alias of {@code model}. */
	public Optional<JustificationElement> element() {
		return model.elementWithId(id);
	}
}
