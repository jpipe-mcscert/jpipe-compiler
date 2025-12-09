package ca.mcscert.exemplars.valid;

import ca.mcscert.exemplars.Counter;
import ca.mcscert.exemplars.SourceFileTest;
import ca.mcscert.jpipe.model.elements.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Source file: src/test/resources/valid/complexMerge.jd")
public class complexMergeIT extends SourceFileTest {

    @Override
    protected String source() { return "valid/complexMerge.jd";}

    @Test
    public void containsTheRightModels(){
        Counter counter = new Counter();
        this.unit.accept(counter);
        assertEquals(3, unit.getContents().size());
    }

    @Nested
    @DisplayName("Composition j")
    class compositionTest {

        @Test
        public void containsTheRightModels() {
            assertTrue(unit.exists("j"));
        }

        @Test
        public void modelsHavetheRightType() {
            assertDoesNotThrow(() -> (Justification) unit.get("j"));
        }

        @Test
        public void justificationContainsRightElements() {
            Counter counter = new Counter();
            unit.get("j").accept(counter);
            assertEquals(1, counter.getAccumulator().get(Counter.Element.EVIDENCE));
            assertEquals(2, counter.getAccumulator().get(Counter.Element.SUB_CONCLUSION));
            assertEquals(3, counter.getAccumulator().get(Counter.Element.STRATEGY));
            assertEquals(1, counter.getAccumulator().get(Counter.Element.CONCLUSION));
        }

        @Test
        public void elementsCanBeAccessedInsideTheJustification() {
            JustificationModel j = unit.get("j");
            assertEquals(j.get("c1"), j.get("j:c1"));
            assertEquals(j.get("AND1"), j.get("j:AND1"));
            assertEquals(j.get("sc1"), j.get("j:sc1"));
            assertEquals(j.get("sc2"), j.get("j:sc2"));
            assertEquals(j.get("s1"), j.get("j:s1"));
            assertEquals(j.get("s2"), j.get("j:s2"));
            assertEquals(j.get("e1"), j.get("j:e1"));
        }

        @Test
        public void elementsHaveTheRightType() {
            JustificationModel j = unit.get("j");
            assertInstanceOf(Conclusion.class, j.get("j:c1"));
            assertInstanceOf(Strategy.class, j.get("j:AND1"));
            assertInstanceOf(SubConclusion.class, j.get("j:sc1"));
            assertInstanceOf(SubConclusion.class, j.get("j:sc2"));
            assertInstanceOf(Strategy.class, j.get("j:s1"));
            assertInstanceOf(Strategy.class, j.get("j:s2"));
            assertInstanceOf(Evidence.class, j.get("j:e1"));
        }

        @Test
        public void elementsAreCorrectlySupported() {
            JustificationModel j = unit.get("j");
            assertEquals(Set.of("AND1"), asIdentifiers(j.get("c1").getSupports()));
            assertEquals(Set.of("sc1", "sc2"), asIdentifiers(j.get("AND1").getSupports()));
            assertEquals(Set.of("s1"), asIdentifiers(j.get("sc1").getSupports()));
            assertEquals(Set.of("s2"), asIdentifiers(j.get("sc2").getSupports()));
            assertEquals(Set.of("e1"), asIdentifiers(j.get("s1").getSupports()));
            assertEquals(Set.of("e1"), asIdentifiers(j.get("s2").getSupports()));
            assertEquals(Set.of(), asIdentifiers(j.get("e1").getSupports()));
        }
    }
}
