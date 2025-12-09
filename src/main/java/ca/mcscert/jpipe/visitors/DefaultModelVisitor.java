package ca.mcscert.jpipe.visitors;

import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;

/**
 * Implements a default visitor for a jPipe model. Mainly provided as example.
 *
 * @param <T> the type used as a "return" from the visit.
 */
public abstract class DefaultModelVisitor<T> extends ModelVisitor<T> {


    protected DefaultModelVisitor(T result) {
        super(result);
    }

    @Override
    public void visit(Conclusion c) {
        c.getStrategy().accept(this);
    }

    @Override
    public void visit(Evidence e) {

    }

    @Override
    public void visit(Strategy s) {
        for (JustificationElement support : s.getSupports()) {
            support.accept(this);
        }
    }

    @Override
    public void visit(SubConclusion sc) {
        sc.getStrategy().accept(this);
    }

    @Override
    public void visit(Justification j) {
        for (JustificationElement je : j.contents()) {
            je.accept(this);
        }
    }

    @Override
    public void visit(Pattern p) {
        for (JustificationElement je : p.contents()) {
            je.accept(this);
        }
    }

    @Override
    public void visit(Unit u) {
        for (JustificationModel justification : u.getContents()) {
            justification.accept(this);
        }
    }


    @Override
    public void visit(AbstractSupport as) {

    }
}
