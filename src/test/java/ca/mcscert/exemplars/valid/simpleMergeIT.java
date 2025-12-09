package ca.mcscert.exemplars.valid;

import ca.mcscert.exemplars.Counter;
import ca.mcscert.exemplars.SourceFileTest;
import ca.mcscert.jpipe.model.elements.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Source file: src/test/resources/valid/simpleMerge.jd")
public class simpleMergeIT extends SourceFileTest {

    @Override
    protected String source() {
        return "valid/simpleMerge.jd";
    }

    @Test
    public void containsTheRightModels() {
        Counter counter = new Counter();
        unit.accept(counter);
        assertEquals(3, unit.getContents().size());
    }

    @Nested
    @DisplayName("Composition j")
    class compositionTest {

        @Test
        public void containsTheRightModels(){ assertTrue(unit.exists("j")); }

        @Test
        public void modelsHavetheRightType() { assertDoesNotThrow(() -> (Justification) unit.get("j")); }

        @Test
        public void justificationContainsRightElements() {
            Counter counter = new Counter();
            unit.get("j").accept(counter);
            assertEquals(2, counter.getAccumulator().get(Counter.Element.EVIDENCE));
            assertEquals(1, counter.getAccumulator().get(Counter.Element.STRATEGY));
            assertEquals(1, counter.getAccumulator().get(Counter.Element.CONCLUSION));
        }

        @Test
        public void elementsCanBeAccessedInsideTheJustification() {
            JustificationModel j = unit.get("j");
            assertEquals(j.get("c1"), j.get("j:c1"));
            assertEquals(j.get("s1"), j.get("j:s1"));
            assertEquals(j.get("e1"), j.get("j:e1"));
            assertEquals(j.get("e2"), j.get("j:e2"));
        }

        @Test
        public void elementsHaveTheRightType() {
            JustificationModel j =  unit.get("j");
            assertInstanceOf(Conclusion.class,   j.get("j:c1"));
            assertInstanceOf(Strategy.class, j.get("j:s1"));
            assertInstanceOf(Evidence.class, j.get("j:e1"));
            assertInstanceOf(Evidence.class, j.get("j:e2"));
        }

        @Test
        public void elementsAreCorrectlySupported() {
            JustificationModel j = unit.get("j");
            assertEquals(Set.of("s1"), asIdentifiers(j.get("c1").getSupports()));
            assertEquals(Set.of("e1", "e2"), asIdentifiers(j.get("s1").getSupports()));
            assertEquals(Set.of(), asIdentifiers(j.get("e1").getSupports()));
            assertEquals(Set.of(), asIdentifiers(j.get("e2").getSupports()));
        }

    }


}
