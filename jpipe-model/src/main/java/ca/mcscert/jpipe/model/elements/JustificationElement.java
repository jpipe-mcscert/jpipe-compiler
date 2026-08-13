package ca.mcscert.jpipe.model.elements;

import ca.mcscert.jpipe.visitor.JustificationVisitor;

/**
 * Sealed root of the element hierarchy. Permits only {@link CommonElement} and
 * {@link AbstractSupport}, ensuring exhaustive pattern matching over all
 * element types.
 */
public sealed interface JustificationElement extends ElementView
		permits CommonElement, AbstractSupport {

	/**
	 * The kebab-case name of this element's kind, as used in user-facing
	 * output: {@code conclusion}, {@code sub-conclusion}, {@code strategy},
	 * {@code evidence}, {@code abstract-support}.
	 *
	 * @return the kind name of this element.
	 */
	String kind();

	default <R> R accept(JustificationVisitor<R> visitor) {
		return switch (this) {
			case Conclusion c -> visitor.visit(c);
			case SubConclusion sc -> visitor.visit(sc);
			case Strategy s -> visitor.visit(s);
			case Evidence e -> visitor.visit(e);
			case AbstractSupport as -> visitor.visit(as);
		};
	}
}
