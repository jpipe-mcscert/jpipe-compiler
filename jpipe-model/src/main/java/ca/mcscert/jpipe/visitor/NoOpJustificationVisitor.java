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
 * Convenience base class for visitors that only need to handle a subset of the
 * model hierarchy. All eight visit methods return {@code null} by default;
 * subclasses override only the methods they care about.
 *
 * <p>
 * Use {@link Void} as the type parameter for side-effect-only subclasses.
 * Subclasses that return a non-{@code null} value must override every method
 * whose return value will be consumed by a caller.
 *
 * @param <R>
 *            return type of each visit method
 */
public abstract class NoOpJustificationVisitor<R>
		implements
			JustificationVisitor<R> {

	@Override
	public R visit(Unit unit) {
		return null;
	}

	@Override
	public R visit(Justification justification) {
		return null;
	}

	@Override
	public R visit(Template template) {
		return null;
	}

	@Override
	public R visit(Conclusion conclusion) {
		return null;
	}

	@Override
	public R visit(SubConclusion subConclusion) {
		return null;
	}

	@Override
	public R visit(Strategy strategy) {
		return null;
	}

	@Override
	public R visit(Evidence evidence) {
		return null;
	}

	@Override
	public R visit(AbstractSupport abstractSupport) {
		return null;
	}
}
