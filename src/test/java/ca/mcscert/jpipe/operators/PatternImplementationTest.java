package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.model.elements.Strategy;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class PatternImplementationTest {

    private Pattern p;
    private Justification j;

    @BeforeEach
    public void initializePattern() {
        p = new Pattern("p");
        Conclusion c      = new Conclusion("c", "A Conclusion");
        Strategy s        = new Strategy("s", "A Strategy");
        AbstractSupport x = new AbstractSupport("x", "An Abstract Support");
        x.supports(s); s.supports(c);
        p.add(x); p.add(s); p.add(c);
    }

    @BeforeEach
    public void initializeJustification() {
        j = new Justification("j");
        j.add(new Evidence("x", "An Evidence"));
    }

    @Test
    public void applyOperatorHaveNoSideEffectsOnParameters() {

        assertEquals(3, p.contents().size());
        assertEquals(1, j.contents().size());

        JustificationModel result = implementsPattern(j, p);

        assertEquals(3, p.contents().size()); // p is untouched
        assertEquals(1, j.contents().size()); // j is untouched

        assertEquals(3, result.contents().size()); // result is the final one
    }

    @Test
    public void resultIsTheRightType() {
        PatternImplementation op = new PatternImplementation();
        // Implements: Justification x Pattern -> Justification
        assertDoesNotThrow(() -> (Justification) implementsPattern(j, p));
        // Implements: Pattern x Pattern -> Pattern
        assertDoesNotThrow(() -> (Pattern) implementsPattern(new Pattern("?"), p));
    }


    @Test
    public void elementsAvailableInTheJustification() {
        PatternImplementation op = new PatternImplementation();
        JustificationModel result = implementsPattern(j, p);
        assertEquals(result.get("c"), result.get("j:c"));
        assertEquals(result.get("s"), result.get("j:s"));
        assertEquals(result.get("x"), result.get("j:x"));
    }

    @Test
    public void modelStructureIsOk() {
        PatternImplementation op = new PatternImplementation();
        JustificationModel result = implementsPattern(j, p);
        assertEquals(Set.of("s"), result.get("c").getSupportingIds());
        assertEquals(Set.of("x"), result.get("s").getSupportingIds());
        assertEquals(Set.of(), result.get("x").getSupportingIds());
    }

    private JustificationModel implementsPattern(JustificationModel j, Pattern p) {
        PatternImplementation op = new PatternImplementation();
        JustificationModel result =
                op.run(CompositionOperator.ReturnType.of(j), j.getName(),
                        List.of(j, p), Map.of());
        return result;
    }



}
