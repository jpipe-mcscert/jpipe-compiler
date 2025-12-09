package ca.mcscert.exemplars.valid;

import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.mcscert.exemplars.Counter;
import ca.mcscert.exemplars.SourceFileTest;
import ca.mcscert.jpipe.model.elements.Justification;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Source file: src/test/resources/valid/simple.jd")
public class SimpleIT extends SourceFileTest {

    @Override
    protected String source() {
        return "valid/simple.jd";
    }

    @Test
    public void containsOneJustification() {
        Counter counter = new Counter();
        this.unit.accept(counter);
        assertEquals(1, unit.getContents().size());
        assertTrue(unit.exists("proving"));
    }

    @Test
    public void modelCanBeCastedAsAJustification() {
        assertDoesNotThrow(() -> (Justification) this.unit.get("proving"));
    }

    @Test
    public void justificationContainsRightElements() {
        Counter counter = new Counter();
        this.unit.get("proving").accept(counter);
        assertEquals(3, counter.getAccumulator().get(Counter.Element.EVIDENCE));
        assertEquals(2, counter.getAccumulator().get(Counter.Element.STRATEGY));
        assertEquals(1, counter.getAccumulator().get(Counter.Element.SUB_CONCLUSION));
        assertEquals(1, counter.getAccumulator().get(Counter.Element.CONCLUSION));
    }

    @Test
    public void elementsCanBeAccessedInsideTheJustification() {
        Justification j = (Justification) this.unit.get("proving");
        assertEquals(j.get("C"),   j.get("proving:C"));
        assertEquals(j.get("St2"), j.get("proving:St2"));
        assertEquals(j.get("St1"), j.get("proving:St1"));
        assertEquals(j.get("Su3"), j.get("proving:Su3"));
        assertEquals(j.get("Su2"), j.get("proving:Su2"));
        assertEquals(j.get("Su1"), j.get("proving:Su1"));
        assertEquals(j.get("SC1"), j.get("proving:SC1"));
    }

    @Test
    public void elementsHaveTheRightType() {
        Justification j = (Justification) this.unit.get("proving");
        assertInstanceOf(Conclusion.class, j.get("C"));
        assertInstanceOf(Strategy.class, j.get("St2"));
        assertInstanceOf(Strategy.class, j.get("St1"));
        assertInstanceOf(Evidence.class, j.get("Su3"));
        assertInstanceOf(Evidence.class, j.get("Su2"));
        assertInstanceOf(Evidence.class, j.get("Su1"));
        assertInstanceOf(SubConclusion.class, j.get("SC1"));
    }



    @Test
    public void elementsAreCorrectlySupported() {
        assertDoesNotThrow(() -> (Justification) this.unit.get("proving"));
        Justification j = (Justification) this.unit.get("proving");

        assertEquals(Set.of("St2"), asIdentifiers(j.get("C").getSupports()));
        assertEquals(Set.of("Su2", "Su3", "SC1"), asIdentifiers(j.get("St2").getSupports()));
        assertEquals(Set.of("Su1"), asIdentifiers(j.get("St1").getSupports()));
        assertEquals(Set.of(), asIdentifiers(j.get("Su3").getSupports()));
        assertEquals(Set.of(), asIdentifiers(j.get("Su2").getSupports()));
        assertEquals(Set.of(), asIdentifiers(j.get("Su1").getSupports()));
        assertEquals(Set.of("St1"), asIdentifiers(j.get("SC1").getSupports()));
    }



}
