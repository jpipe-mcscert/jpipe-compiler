package ca.mcscert.jpipe.model.elements;

/**
 * Elements that can act as direct support in a justification graph:
 * {@link SubConclusion}, {@link Evidence}, and {@link AbstractSupport}.
 *
 * <p>
 * Extends {@link ElementView} so that a supporter's id and label can be read
 * without casting it back to a {@link JustificationElement}.
 */
public interface SupportLeaf extends ElementView {
}
