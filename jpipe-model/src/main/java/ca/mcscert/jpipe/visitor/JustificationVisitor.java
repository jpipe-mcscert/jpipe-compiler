package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;

/**
 * Visitor over the jPipe model hierarchy. Type parameter {@code R} is the
 * return type of each visit method; use {@link Void} for side-effect-only
 * visitors.
 */
public interface JustificationVisitor<R> {

	R visit(Unit unit);

	R visit(Justification justification);

	R visit(Template template);

	R visit(Conclusion conclusion);

	R visit(SubConclusion subConclusion);

	R visit(Strategy strategy);

	R visit(Evidence evidence);

	R visit(AbstractSupport abstractSupport);
}
