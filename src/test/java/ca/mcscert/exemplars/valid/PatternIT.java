package ca.mcscert.exemplars.valid;

import ca.mcscert.exemplars.Counter;
import ca.mcscert.exemplars.SourceFileTest;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Source file: src/test/resources/valid/pattern.jd")
public class PatternIT extends SourceFileTest {

    @Override
    protected String source() {
        return "valid/pattern.jd";
    }

    @Test
    public void containsTheRightModels() {
        Counter counter = new Counter();
        unit.accept(counter);
        assertEquals(3, unit.getContents().size());
    }


    @Nested
    @DisplayName("Pattern 'prover'")
    class ProverPatternTest {

        @Test
        public void containsTheRightModels() {
            assertTrue(unit.exists("prover"));
        }

        @Test
        public void modelsHaveTheRightType() {
            assertDoesNotThrow(() -> (Pattern) unit.get("prover"));
        }

        @Test
        public void patternContainsRightElements() {
            Counter counter = new Counter();
            unit.get("prover").accept(counter);
            assertEquals(1, counter.getAccumulator().get(Counter.Element.EVIDENCE));
            assertEquals(1, counter.getAccumulator().get(Counter.Element.STRATEGY));
            assertEquals(2, counter.getAccumulator().get(Counter.Element.ABSTRACT));
            assertEquals(1, counter.getAccumulator().get(Counter.Element.CONCLUSION));
        }

        @Test
        public void elementsCanBeAccessedInsideThePattern() {
            JustificationModel j = unit.get("prover");
            assertEquals(j.get("C"),   j.get("prover:C"));
            assertEquals(j.get("St1"), j.get("prover:St1"));
            assertEquals(j.get("Su3"), j.get("prover:Su3"));
            assertEquals(j.get("Su2"), j.get("prover:Su2"));
            assertEquals(j.get("Su1"), j.get("prover:Su1"));
        }

        @Test
        public void elementsHaveTheRightType() {
            JustificationModel j =  unit.get("prover");
            assertInstanceOf(Conclusion.class, j.get("C"));
            assertInstanceOf(Strategy.class, j.get("St1"));
            assertInstanceOf(Evidence.class, j.get("Su3"));
            assertInstanceOf(AbstractSupport.class, j.get("Su2"));
            assertInstanceOf(AbstractSupport.class, j.get("Su1"));
        }

        @Test
        public void elementsAreCorrectlySupported() {
            JustificationModel j = unit.get("prover");
            assertEquals(Set.of("St1"), asIdentifiers(j.get("C").getSupports()));
            assertEquals(Set.of("Su1", "Su2", "Su3"), asIdentifiers(j.get("St1").getSupports()));
            assertEquals(Set.of(), asIdentifiers(j.get("Su3").getSupports()));
            assertEquals(Set.of(), asIdentifiers(j.get("Su2").getSupports()));
            assertEquals(Set.of(), asIdentifiers(j.get("Su1").getSupports()));
        }

    }

    @Nested
    @DisplayName("Pattern 'partial'")
    class PartialPatternTest {

        @Test
        public void containsTheRightModels() {
            assertTrue(unit.exists("partial"));
        }

        @Test
        public void modelsHaveTheRightType() {
            assertDoesNotThrow(() -> (Pattern) unit.get("partial"));
        }

        @Test
        public void patternContainsRightElements() {
            Counter counter = new Counter();
            unit.get("partial").accept(counter);
            assertEquals(2, counter.getAccumulator().get(Counter.Element.EVIDENCE));
            assertEquals(1, counter.getAccumulator().get(Counter.Element.STRATEGY));
            assertEquals(1, counter.getAccumulator().get(Counter.Element.ABSTRACT));
            assertEquals(1, counter.getAccumulator().get(Counter.Element.CONCLUSION));
        }

        @Test
        public void elementsCanBeAccessedInsideThePattern() {
            JustificationModel j = unit.get("partial");
            assertEquals(j.get("C"),   j.get("partial:C"));
            assertEquals(j.get("St1"), j.get("partial:St1"));
            assertEquals(j.get("Su3"), j.get("partial:Su3"));
            assertEquals(j.get("Su2"), j.get("partial:Su2"));
            assertEquals(j.get("Su1"), j.get("partial:Su1"));
        }

        @Test
        public void elementsHaveTheRightType() {
            JustificationModel j =  unit.get("partial");
            assertInstanceOf(Conclusion.class, j.get("C"));
            assertInstanceOf(Strategy.class, j.get("St1"));
            assertInstanceOf(Evidence.class, j.get("Su3"));
            assertInstanceOf(Evidence.class, j.get("Su2"));
            assertInstanceOf(AbstractSupport.class, j.get("Su1"));
        }

        @Test
        public void elementsAreCorrectlySupported() {
            JustificationModel j = unit.get("partial");
            assertEquals(Set.of("St1"), asIdentifiers(j.get("C").getSupports()));
            assertEquals(Set.of("Su1", "Su2", "Su3"), asIdentifiers(j.get("St1").getSupports()));
            assertEquals(Set.of(), asIdentifiers(j.get("Su3").getSupports()));
            assertEquals(Set.of(), asIdentifiers(j.get("Su2").getSupports()));
            assertEquals(Set.of(), asIdentifiers(j.get("Su1").getSupports()));
        }

    }

    @Nested
    @DisplayName("Justification 'proving'")
    class ProvingJustificationTest {

        @Test
        public void containsTheRightModels() {
            assertTrue(unit.exists("proving"));
        }

        @Test
        public void modelsHaveTheRightType() {
            assertDoesNotThrow(() -> (Justification) unit.get("proving"));
        }

        @Test
        public void patternContainsRightElements() {
            Counter counter = new Counter();
            unit.get("proving").accept(counter);
            assertEquals(3, counter.getAccumulator().getOrDefault(Counter.Element.EVIDENCE,0));
            assertEquals(2, counter.getAccumulator().getOrDefault(Counter.Element.STRATEGY,0));
            assertEquals(0, counter.getAccumulator().getOrDefault(Counter.Element.ABSTRACT,0));
            assertEquals(1, counter.getAccumulator().getOrDefault(Counter.Element.SUB_CONCLUSION,0));
            assertEquals(1, counter.getAccumulator().getOrDefault(Counter.Element.CONCLUSION,0));
        }

        @Test
        public void elementsCanBeAccessed() {
            JustificationModel j = unit.get("proving");
            assertEquals(j.get("C"),   j.get("proving:C"));
            assertEquals(j.get("St0"), j.get("proving:St0"));
            assertEquals(j.get("St1"), j.get("proving:St1"));
            assertEquals(j.get("Su0"), j.get("proving:Su0"));
            assertEquals(j.get("Su1"), j.get("proving:Su1"));
            assertEquals(j.get("Su2"), j.get("proving:Su2"));
            assertEquals(j.get("Su3"), j.get("proving:Su3"));
        }

        @Test
        public void elementsHaveTheRightType() {
            JustificationModel j =  unit.get("proving");
            assertInstanceOf(Conclusion.class, j.get("C"));
            assertInstanceOf(Strategy.class, j.get("St0"));
            assertInstanceOf(Strategy.class, j.get("St1"));
            assertInstanceOf(Evidence.class, j.get("Su3"));
            assertInstanceOf(Evidence.class, j.get("Su2"));
            assertInstanceOf(SubConclusion.class, j.get("Su1"));
            assertInstanceOf(Evidence.class, j.get("Su0"));
        }

        @Test
        public void elementsAreCorrectlySupported() {
            JustificationModel j = unit.get("proving");
            assertEquals(Set.of("St1"), j.get("C").getSupportingIds());
            assertEquals(Set.of("Su0"), j.get("St0").getSupportingIds());
            assertEquals(Set.of("Su1", "Su2", "Su3"), j.get("St1").getSupportingIds());
            assertEquals(Set.of(), j.get("Su0").getSupportingIds());
            assertEquals(Set.of("St0"), j.get("Su1").getSupportingIds());
            assertEquals(Set.of(), j.get("Su2").getSupportingIds());
            assertEquals(Set.of(), j.get("Su3").getSupportingIds());
        }

    }


}
