package ca.mcscert.jpipe.compiler.steps.checkers;

import ca.mcscert.jpipe.compiler.model.Checker;
import ca.mcscert.jpipe.error.ErrorManager;
import ca.mcscert.jpipe.error.SemanticError;
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
import ca.mcscert.jpipe.visitors.ModelVisitor;
import java.util.ArrayList;
import java.util.List;

/**
 * Check that a model is consistent. Record errors in the global error manager if not.
 * Being "not complete" is not a deal-breaker per se.
 */
public final class ConsistencyChecker extends Checker<Unit> {

    @Override
    protected void check(Unit input, String source) {
        ConsistencyVisitor visitor = new ConsistencyVisitor();
        input.accept(visitor);

        List<Throwable> errors = visitor.getAccumulator();
        if (! errors.isEmpty()) {
            errors.forEach(ErrorManager.getInstance()::registerError);
        }
    }

    private static class ConsistencyVisitor extends ModelVisitor<List<Throwable>> {

        private Scope current;

        public ConsistencyVisitor() {
            super(new ArrayList<>());
        }

        @Override
        public void visit(Unit u) {
            for (JustificationModel justification : u.getContents()) {
                justification.accept(this);
            }
        }

        @Override
        public void visit(Justification j) {
            this.current = Scope.JUSTIFICATION;
            for (JustificationElement je : j.contents()) {
                je.accept(this);
            }
            this.current = Scope.NONE;
        }

        @Override
        public void visit(Pattern p) {
            this.current = Scope.PATTERN;
            for (JustificationElement je : p.contents()) {
                je.accept(this);
            }
            this.current = Scope.NONE;
        }

        @Override
        public void visit(AbstractSupport as) {
            if (this.current != Scope.PATTERN) {
               accumulator.add(
                       new SemanticError("Abstract support can't be used outside of a pattern ("
                               + as.getIdentifier() + ")"));
            }
        }

        @Override
        public void visit(Conclusion c) { }

        @Override
        public void visit(Evidence e) { }

        @Override
        public void visit(Strategy s) { }

        @Override
        public void visit(SubConclusion sc) { }
    }

    private enum Scope {
        JUSTIFICATION, PATTERN, NONE
    }

}
