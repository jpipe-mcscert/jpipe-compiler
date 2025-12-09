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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Check that a model is complete. Record errors in the global error manager if not.
 * Being "not complete" is not a deal-breaker per se. We delegate to "HaltAndCatchFire" the decision
 * to stop the compilation process if error where recorded.
 */
public final class CompletenessChecker extends Checker<Unit> {

    @Override
    protected void check(Unit input, String source) {
        CompletenessVisitor visitor = new CompletenessVisitor();
        input.accept(visitor);

        List<Throwable> errors = visitor.getAccumulator();
        if (! errors.isEmpty()) {
            errors.forEach(ErrorManager.getInstance()::registerError);
        }
    }

    private static class CompletenessVisitor extends ModelVisitor<List<Throwable>> {

        private final Set<JustificationElement> visited;
        private final Set<JustificationElement> used;

        public CompletenessVisitor() {
            super(new ArrayList<>());
            this.visited = new HashSet<>();
            this.used = new HashSet<>();
        }

        @Override
        public void visit(Unit u) {
            for (JustificationModel justification : u.getContents()) {
                justification.accept(this);
            }
            if (!this.used.containsAll(this.visited)) {
                // some visited elements are not part of the global justification
                this.visited.removeAll(this.used);
                for (JustificationElement je : this.visited) {
                    accumulator.add(new SemanticError("Dangling justification element: ["
                                                    + je.getIdentifier() + "]"));
                }
            }
        }

        @Override
        public void visit(Justification j) {
            // we're visiting all declared symbols, not just the entry point (the conclusion)
            for (JustificationElement je : j.contents()) {
                je.accept(this);
            }
        }

        @Override
        public void visit(Pattern p) {
            // we're visiting all declared symbols, not just the entry point (the conclusion)
            for (JustificationElement je : p.contents()) {
                je.accept(this);
            }
        }

        @Override
        public void visit(Conclusion c) {
            this.visited.add(c);
            this.used.add(c); // conclusion is always "used": it's the output of the justification.
            if (c.getStrategy() == null) {
                accumulator.add(new SemanticError("[" + c + "] is incomplete (no support)"));
            } else {
                this.used.add(c.getStrategy());
            }
        }

        @Override
        public void visit(Evidence e) {
            this.visited.add(e);
        }

        @Override
        public void visit(Strategy s) {
            this.visited.add(s);
            if (s.getSupports().isEmpty()) {
                accumulator.add(new SemanticError("[" + s + "] is incomplete (no support)"));
            } else {
                this.used.addAll(s.getSupports());
            }
        }

        @Override
        public void visit(SubConclusion sc) {
            this.visited.add(sc);
            if (sc.getStrategy() == null) {
                accumulator.add(new SemanticError("[" + sc + "] is incomplete (no support)"));
            } else {
                this.used.add(sc.getStrategy());
            }
        }

        @Override
        public void visit(AbstractSupport as) {

        }

    }

}
