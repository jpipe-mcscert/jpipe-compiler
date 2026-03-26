package ca.mcscert.jpipe.model.elements;

import java.util.Optional;

/**
 * Capability interface for elements that are supported by exactly one other
 * element in a justification graph. The type parameter {@code T} constrains
 * which element types are valid supporters.
 *
 * <ul>
 * <li>{@link Conclusion} and {@link SubConclusion} are supported by exactly one
 * {@link Strategy}.</li>
 * <li>{@link Strategy} is supported by exactly one {@link SupportLeaf} element
 * ({@link SubConclusion}, {@link Evidence}, {@link AbstractSupport}).</li>
 * </ul>
 */
public interface Supportable<T> {

	void addSupport(T supporter);

	Optional<T> getSupport();
}
