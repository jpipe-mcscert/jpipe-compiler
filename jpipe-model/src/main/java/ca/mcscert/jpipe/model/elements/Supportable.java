package ca.mcscert.jpipe.model.elements;

import java.util.List;

/**
 * Capability interface for elements that can be supported by other elements
 * in a justification graph. The type parameter {@code T} constrains which
 * element types are valid supporters.
 *
 * <ul>
 *   <li>{@link Conclusion} and {@link SubConclusion} are supported by
 *       {@link Strategy}.</li>
 *   <li>{@link Strategy} is supported by {@link SupportLeaf} elements
 *       ({@link SubConclusion}, {@link Evidence}, {@link AbstractSupport}).</li>
 * </ul>
 */
public interface Supportable<T> {

	void addSupport(T supporter);

	List<T> getSupporters();
}
