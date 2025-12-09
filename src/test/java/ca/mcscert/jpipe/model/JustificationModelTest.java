package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.model.elements.Strategy;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class JustificationModelTest {

    private JustificationModel justification;
    private JustificationModel pattern;

    @BeforeEach
    public void initializeJustification() {
        justification = new Justification("a justification");

        Conclusion c = new Conclusion("c", "This is my conclusion");
        justification.add(c);

        Strategy s = new Strategy("s", "this is a strategy");
        justification.add(s);
        s.supports(c);

        Evidence e = new Evidence("e", "this is an evidence");
        justification.add(e);
        e.supports(s);

        justification.freeze();
    }

    @BeforeEach
    public void initializePattern() {
        pattern = new Pattern("a pattern");

        Conclusion c = new Conclusion("c", "This is my conclusion");
        pattern.add(c);

        Strategy s = new Strategy("s", "this is a strategy");
        pattern.add(s);
        s.supports(c);

        AbstractSupport ae = new AbstractSupport("ae", "this is an abstract support");
        pattern.add(ae);
        ae.supports(s);

        pattern.freeze();
    }

    @Test
    public void contentsCanBeAccessed() {
        assertEquals(3, justification.contents().size());
        assertEquals(3, pattern.contents().size());
    }

    @Test
    public void hierarchyIsPreserved() {
        assertEquals(3, justification.contents().size());
        assertEquals(Set.of("s"), justification.get("c").getSupports().stream()
                .map(JustificationElement::getIdentifier)
                .collect(Collectors.toSet()));
        assertEquals(Set.of("e"), justification.get("s").getSupports().stream()
                .map(JustificationElement::getIdentifier)
                .collect(Collectors.toSet()));
    }


    @Test
    public void cloneKeepHierarchy()  {
        JustificationModel m =  justification.replicate();
        assertEquals(3, m.contents().size());
        assertEquals(Set.of("s"), m.get("c").getSupports().stream()
                                        .map(JustificationElement::getIdentifier)
                                        .collect(Collectors.toSet()));
        assertEquals(Set.of("e"), m.get("s").getSupports().stream()
                .map(JustificationElement::getIdentifier)
                .collect(Collectors.toSet()));
    }


    @Test
    public void shallowCloningOfElementaryArtefacts() {
        Conclusion c = new Conclusion("c", "This is my conclusion");
        Strategy   s = new Strategy("s", "This is a strategy");
        s.supports(c);

        Conclusion cP = (Conclusion) c.shallow();
        assertEquals("This is my conclusion", cP.getLabel());
        assertNull(cP.getStrategy());
        assertEquals(0, cP.getSupports().size());

        Strategy sP = new Strategy("s", "This is another strategy");
        sP.supports(cP);
        assertEquals("This is a strategy", c.getStrategy().getLabel());
        assertEquals("This is another strategy", cP.getStrategy().getLabel());
    }

}
