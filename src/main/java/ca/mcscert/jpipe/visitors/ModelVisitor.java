package ca.mcscert.jpipe.visitors;

import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;

/**
 * Define what a Visitor is for jPipe models. We are using "typed" visitors, i.e., visitors that
 * produce an output as the result of their visit. This "result" is defined as an accumulator.
 *
 * @param <O> the output type of this visitor.
 */
public abstract class ModelVisitor<O> {

    protected final O accumulator;

    /**
     * Instantiate a new visitor with its accumulator result (usually provided empty).
     *
     * @param accumulator the accumulating variable to use when visiting the model.
     */
    protected ModelVisitor(O accumulator) {
        this.accumulator = accumulator;
    }

    public final O getAccumulator() {
        return accumulator;
    }

    /**
     * Visiting a Conclusion.
     *
     * @param c the conclusion to visit.
     */
    public abstract void visit(Conclusion c);

    /**
     * Visiting an Evidence.
     *
     * @param e the evidence to visit.
     */
    public abstract void visit(Evidence e);

    /**
     *  Visiting a Strategy.
     *
     * @param s the strategy to visit.
     */
    public abstract void visit(Strategy s);

    /**
     * Visiting a sub-conclusion.
     *
     * @param sc the sub-conclusion to visit.
     */
    public abstract void visit(SubConclusion sc);

    /**
     * Visiting an abstract support.
     *
     * @param as the abstract support to visit.
     */
    public abstract void visit(AbstractSupport as);

    /**
     * Visiting a justification.
     *
     * @param j the justification to visit.
     */
    public abstract void visit(Justification j);

    /**
     * Visiting a justification pattern.
     *
     * @param p the pattern to visit.
     */
    public abstract void visit(Pattern p);

    /**
     * Visiting a compilation unit.
     *
     * @param u the unit to visit.
     */
    public abstract void visit(Unit u);

}
