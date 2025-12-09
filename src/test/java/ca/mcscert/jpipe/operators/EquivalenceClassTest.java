package ca.mcscert.jpipe.operators;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.operators.internals.EquivalenceClass;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class EquivalenceClassTest {

    private JustificationModel j1;
    private JustificationModel j2;

    @BeforeEach
    public void initialize() {
        // Initializing j1
        j1 = new Justification("j1");
        Evidence e1 = new Evidence("e1", "A unique Evidence");
        j1.add(e1);
        Strategy s1 = new Strategy("s1", "A shared Strategy");
        e1.supports(s1);
        j1.add(s1);
        Conclusion c1 = new Conclusion("c1", "A common strategy");
        j1.add(c1);
        s1.supports(c1);

        // Initializing j2
        j2 = new Justification("j1");
        Evidence e2 = new Evidence("e2", "Another unique Evidence");
        j2.add(e2);
        Strategy s2 = new Strategy("s2", "A shared Strategy");
        e2.supports(s2);
        j2.add(s2);
        Conclusion c2 = new Conclusion("c2", "A common strategy");
        j2.add(c2);
        s2.supports(c2);
    }

    @Test
    public void equivalenceReflexivity() {
        EquivalenceClass eq = new EquivalenceClass();
        Map<String, String> mapping = eq.apply(j1, j1);
        assertEquals(3, mapping.size());
        for (String key : mapping.keySet()) {
            assertEquals(key, mapping.get(key));
        }
    }

    @Test
    public void identifyEquivalentElements() {
        EquivalenceClass eq = new EquivalenceClass();
        Map<String, String> mapping = eq.apply(j1, j2);
        assertEquals(2, mapping.size());
        assertEquals("c2", mapping.get("c1"));
        assertEquals("s2", mapping.get("s1"));
    }

}
