package ca.mcscert.exemplars.valid;

import ca.mcscert.exemplars.Counter;
import ca.mcscert.exemplars.SourceFileTest;
import ca.mcscert.jpipe.model.elements.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Source file: src/test/resources/valid/mergedAssembly.jd")
public class mergedAssemblyIT extends SourceFileTest {
    @Override
    protected String source() {
        return "valid/mergedAssembly.jd";
    }

    @Test
    public void containsOneJustification() {
        Counter counter = new Counter();
        this.unit.accept(counter);
        assertEquals(3, unit.getContents().size());
    }

    @Nested
    @DisplayName("Composition 'j'")
    class ProvingJustificationTest {
        @Test
        public void containsTheRightModels() {
            assertTrue(unit.exists("j"));
        }

        @Test
        public void modelsHaveTheRightType() {
            assertDoesNotThrow(() -> (Justification) unit.get("j"));
        }

        @Test
        public void patternContainsRightElements() {
            Counter counter = new Counter();
            unit.get("j").accept(counter);
            assertEquals(1, counter.getAccumulator().getOrDefault(Counter.Element.EVIDENCE,0));
            assertEquals(3, counter.getAccumulator().getOrDefault(Counter.Element.STRATEGY,0));
            assertEquals(2, counter.getAccumulator().getOrDefault(Counter.Element.SUB_CONCLUSION,0));
            assertEquals(1, counter.getAccumulator().getOrDefault(Counter.Element.CONCLUSION,0));
        }

        @Test
        public void elementsCanBeAccessed() {
            JustificationModel j = unit.get("j");
            assertEquals(j.get("c"),   j.get("j:c"));
            assertEquals(j.get("AND"), j.get("j:AND"));
            assertEquals(j.get("c1"), j.get("j:c1"));
            assertEquals(j.get("c2"), j.get("j:c2"));
            assertEquals(j.get("s1"), j.get("j:s1"));
            assertEquals(j.get("s2"), j.get("j:s2"));
            assertEquals(j.get("e1"), j.get("j:e1"));
        }

        @Test
        public void elementsHaveTheRightType() {
            JustificationModel j = unit.get("j");
            assertInstanceOf(Conclusion.class,   j.get("j:c"));
            assertInstanceOf(Strategy.class, j.get("j:AND"));
            assertInstanceOf(SubConclusion.class, j.get("j:c1"));
            assertInstanceOf(SubConclusion.class, j.get("j:c2"));
            assertInstanceOf(Strategy.class, j.get("j:s1"));
            assertInstanceOf(Strategy.class, j.get("j:s2"));
            assertInstanceOf(Evidence.class, j.get("j:e1"));
        }

        @Test
        public void elementsAreCorrectlySupported() {
            JustificationModel j = unit.get("j");
            assertEquals(Set.of("AND"), j.get("c").getSupportingIds());
            assertEquals(Set.of("c1", "c2"), j.get("AND").getSupportingIds());
            assertEquals(Set.of("s1"), j.get("c1").getSupportingIds());
            assertEquals(Set.of("s2"), j.get("c2").getSupportingIds());
            assertEquals(Set.of("e1"), j.get("s1").getSupportingIds());
            assertEquals(Set.of("e1"), j.get("s2").getSupportingIds());
            assertEquals(Set.of(), j.get("e1").getSupportingIds());
        }
    }
}
