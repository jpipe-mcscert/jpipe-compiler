package ca.mcscert.exemplars;

import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.visitors.ModelVisitor;
import java.util.HashMap;
import java.util.Map;

public class Counter extends ModelVisitor<Map<Counter.Element, Integer>> {

    public Counter() { super(new HashMap<>()); }

    @Override public void visit(Unit u) {  u.getContents().forEach((e) -> e.accept(this));   }

    @Override public void visit(Justification j) {
        inc(Element.JUSTIFICATION);
        j.contents().forEach((e) -> e.accept(this));
    }

    @Override public void visit(Pattern p) {
        inc(Element.PATTERN);
        p.contents().forEach((e) -> e.accept(this));
    }

    @Override public void visit(Conclusion c)       { inc(Element.CONCLUSION);     }
    @Override public void visit(Evidence e)         { inc(Element.EVIDENCE);       }
    @Override public void visit(Strategy s)         { inc(Element.STRATEGY);       }
    @Override public void visit(SubConclusion sc)   { inc(Element.SUB_CONCLUSION); }
    @Override public void visit(AbstractSupport as) { inc(Element.ABSTRACT);       }

    private void inc(Element element) {
        int val = this.accumulator.getOrDefault(element, 0);
        this.accumulator.put(element, val + 1);
    }

    public enum Element {
        JUSTIFICATION, PATTERN,
        EVIDENCE, STRATEGY, SUB_CONCLUSION, CONCLUSION, ABSTRACT
    }

}
