package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.Visitable;
import ca.mcscert.jpipe.visitors.ModelVisitor;

/**
 * Step executing a given visitor (pattern) on a jPipe model.
 *
 * @param <I> the input type, must be Visitable.
 * @param <O> the output type (produced by the provided visitor).
 */
public final class ModelVisit<I extends Visitable, O> extends Transformation<I, O> {

    private final ModelVisitor<O> visitor;

    /**
     * When building this compilation step, we provide the concrete visitor to use.
     *
     * @param visitor and instance of visitor producing an O as output.
     */
    public ModelVisit(ModelVisitor<O> visitor) {
        this.visitor = visitor;
    }

    @Override
    protected O run(I input, String source) throws Exception {
        input.accept(visitor);
        return visitor.getAccumulator();
    }

}
