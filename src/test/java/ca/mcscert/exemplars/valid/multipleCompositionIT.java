package ca.mcscert.exemplars.valid;

import ca.mcscert.exemplars.Counter;
import ca.mcscert.exemplars.SourceFileTest;
import ca.mcscert.jpipe.model.elements.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@DisplayName("Source file: src/test/resources/valid/multipleComposition.jd")
public class multipleCompositionIT extends SourceFileTest {
    @Override
    protected String source() {
        return "valid/multipleComposition.jd";
    }

    @Test
    public void containsOneJustification() {
        Counter counter = new Counter();
        this.unit.accept(counter);
        assertEquals(8, unit.getContents().size());
    }

    @Nested
    @DisplayName("Assemble 'recruitment'")
    class AssembleRecruitmentTest {

        @Test
        public void containsTheRightModels() {
            assertTrue(unit.exists("recruitment"));
        }

        @Test
        public void modelsHaveTheRightType() {
            assertDoesNotThrow(() -> (Justification) unit.get("recruitment"));
        }

        @Test
        public void patternContainsRightElements() {
            Counter counter = new Counter();
            unit.get("recruitment").accept(counter);
            assertEquals(5, counter.getAccumulator().getOrDefault(Counter.Element.EVIDENCE,0));
            assertEquals(4, counter.getAccumulator().getOrDefault(Counter.Element.STRATEGY,0));
            assertEquals(3, counter.getAccumulator().getOrDefault(Counter.Element.SUB_CONCLUSION,0));
            assertEquals(1, counter.getAccumulator().getOrDefault(Counter.Element.CONCLUSION,0));
        }

        @Test
        public void elementsCanBeAccessed() {
            JustificationModel j = unit.get("recruitment");
            assertEquals(j.get("c"),   j.get("recruitment:c"));
            assertEquals(j.get("AND"), j.get("recruitment:AND"));
            assertEquals(j.get("c1"), j.get("recruitment:c1"));
            assertEquals(j.get("c2"), j.get("recruitment:c2"));
            assertEquals(j.get("c3"), j.get("recruitment:c3"));
            assertEquals(j.get("calling"), j.get("recruitment:calling"));
            assertEquals(j.get("script"), j.get("recruitment:script"));
            assertEquals(j.get("directory"), j.get("recruitment:directory"));
            assertEquals(j.get("display"), j.get("recruitment:display"));
            assertEquals(j.get("locations"), j.get("recruitment:locations"));
            assertEquals(j.get("poster"), j.get("recruitment:poster"));
            assertEquals(j.get("chat"), j.get("recruitment:chat"));
            assertEquals(j.get("talking_points"), j.get("recruitment:talking_points"));
        }

        @Test
        public void elementsHaveTheRightType() {
            JustificationModel j = unit.get("recruitment");
            assertInstanceOf(Conclusion.class, j.get("recruitment:c"));
            assertInstanceOf(Strategy.class, j.get("recruitment:calling"));
            assertInstanceOf(SubConclusion.class, j.get("recruitment:c1"));
            assertInstanceOf(SubConclusion.class, j.get("recruitment:c2"));
            assertInstanceOf(SubConclusion.class, j.get("recruitment:c3"));
            assertInstanceOf(Strategy.class, j.get("recruitment:calling"));
            assertInstanceOf(Strategy.class, j.get("recruitment:display"));
            assertInstanceOf(Strategy.class, j.get("recruitment:chat"));
            assertInstanceOf(Evidence.class, j.get("recruitment:script"));
            assertInstanceOf(Evidence.class, j.get("recruitment:directory"));
            assertInstanceOf(Evidence.class, j.get("recruitment:locations"));
            assertInstanceOf(Evidence.class, j.get("recruitment:poster"));
            assertInstanceOf(Evidence.class, j.get("recruitment:talking_points"));
        }

        @Test
        public void elementsAreCorrectlySupported() {
            JustificationModel j = unit.get("recruitment");
            assertEquals(Set.of("AND"),   j.get("recruitment:c").getSupportingIds());
            assertEquals(Set.of("c1", "c2", "c3"), j.get("recruitment:AND").getSupportingIds());
            assertEquals(Set.of("calling"), j.get("recruitment:c1").getSupportingIds());
            assertEquals(Set.of("display"), j.get("recruitment:c2").getSupportingIds());
            assertEquals(Set.of("chat"), j.get("recruitment:c3").getSupportingIds());
            assertEquals(Set.of("script", "directory"), j.get("recruitment:calling").getSupportingIds());
            assertEquals(Set.of(), j.get("recruitment:script").getSupportingIds());
            assertEquals(Set.of(), j.get("recruitment:directory").getSupportingIds());
            assertEquals(Set.of("locations", "poster"), j.get("recruitment:display").getSupportingIds());
            assertEquals(Set.of(), j.get("recruitment:locations").getSupportingIds());
            assertEquals(Set.of(), j.get("recruitment:poster").getSupportingIds());
            assertEquals(Set.of("talking_points", "locations"), j.get("recruitment:chat").getSupportingIds());
            assertEquals(Set.of(), j.get("recruitment:talking_points").getSupportingIds());
        }

    }

    @Nested
    @DisplayName("Refine 'temp'")
    class RefineTempTest {
        @Test
        public void containsTheRightModels() {
            assertTrue(unit.exists("temp"));
        }

        @Test
        public void modelsHaveTheRightType() {
            assertDoesNotThrow(() -> (Justification) unit.get("temp"));
        }

        @Test
        public void patternContainsRightElements() {
            Counter counter = new Counter();
            unit.get("temp").accept(counter);
            assertEquals(6, counter.getAccumulator().getOrDefault(Counter.Element.EVIDENCE,0));
            assertEquals(6, counter.getAccumulator().getOrDefault(Counter.Element.STRATEGY,0));
            assertEquals(5, counter.getAccumulator().getOrDefault(Counter.Element.SUB_CONCLUSION,0));
            assertEquals(1, counter.getAccumulator().getOrDefault(Counter.Element.CONCLUSION,0));
        }

        @Test
        public void elementsCanBeAccessed() {
            JustificationModel j = unit.get("temp");
            assertEquals(j.get("c"),   j.get("temp:c"));
            assertEquals(j.get("AND"), j.get("temp:AND"));
            assertEquals(j.get("c1"), j.get("temp:c1"));
            assertEquals(j.get("c2"), j.get("temp:c2"));
            assertEquals(j.get("c3"), j.get("temp:c3"));
            assertEquals(j.get("calling"), j.get("temp:calling"));
            assertEquals(j.get("script"), j.get("temp:script"));
            assertEquals(j.get("directory"), j.get("temp:directory"));
            assertEquals(j.get("display"), j.get("temp:display"));
            assertEquals(j.get("poster"), j.get("temp:poster"));
            assertEquals(j.get("chat"), j.get("temp:chat"));
            assertEquals(j.get("talking_points"), j.get("temp:talking_points"));
            assertEquals(j.get("loc_available"), j.get("temp:loc_available"));
            assertEquals(j.get("combining"), j.get("temp:combining"));
            assertEquals(j.get("public"), j.get("temp:public"));
            assertEquals(j.get("authorized"), j.get("temp:authorized"));
            assertEquals(j.get("authorization"), j.get("temp:authorization"));
            assertEquals(j.get("places"), j.get("temp:places"));
        }

        @Test
        public void elementsHaveTheRightType() {
            JustificationModel j = unit.get("temp");
            assertInstanceOf(Conclusion.class, j.get("temp:c"));
            assertInstanceOf(Strategy.class, j.get("temp:calling"));
            assertInstanceOf(SubConclusion.class, j.get("temp:c1"));
            assertInstanceOf(SubConclusion.class, j.get("temp:c2"));
            assertInstanceOf(SubConclusion.class, j.get("temp:c3"));
            assertInstanceOf(SubConclusion.class, j.get("temp:loc_available"));
            assertInstanceOf(SubConclusion.class, j.get("temp:authorized"));
            assertInstanceOf(Strategy.class, j.get("temp:calling"));
            assertInstanceOf(Strategy.class, j.get("temp:display"));
            assertInstanceOf(Strategy.class, j.get("temp:chat"));
            assertInstanceOf(Strategy.class, j.get("temp:combining"));
            assertInstanceOf(Strategy.class, j.get("temp:authorization"));
            assertInstanceOf(Evidence.class, j.get("temp:script"));
            assertInstanceOf(Evidence.class, j.get("temp:directory"));
            assertInstanceOf(Evidence.class, j.get("temp:poster"));
            assertInstanceOf(Evidence.class, j.get("temp:talking_points"));
            assertInstanceOf(Evidence.class, j.get("temp:public"));
            assertInstanceOf(Evidence.class, j.get("temp:places"));
        }

        @Test
        public void elementsAreCorrectlySupported() {
            JustificationModel j = unit.get("temp");
            assertEquals(Set.of("AND"),   j.get("temp:c").getSupportingIds());
            assertEquals(Set.of("c1", "c2", "c3"), j.get("temp:AND").getSupportingIds());
            assertEquals(Set.of("calling"), j.get("temp:c1").getSupportingIds());
            assertEquals(Set.of("display"), j.get("temp:c2").getSupportingIds());
            assertEquals(Set.of("chat"), j.get("temp:c3").getSupportingIds());
            assertEquals(Set.of("script", "directory"), j.get("temp:calling").getSupportingIds());
            assertEquals(Set.of(), j.get("temp:script").getSupportingIds());
            assertEquals(Set.of(), j.get("temp:directory").getSupportingIds());
            assertEquals(Set.of("loc_available", "poster"), j.get("temp:display").getSupportingIds());
            assertEquals(Set.of(), j.get("temp:poster").getSupportingIds());
            assertEquals(Set.of("talking_points", "loc_available"), j.get("temp:chat").getSupportingIds());
            assertEquals(Set.of(), j.get("temp:talking_points").getSupportingIds());
            assertEquals(Set.of("combining"), j.get("temp:loc_available").getSupportingIds());
            assertEquals(Set.of("public", "authorized"), j.get("temp:combining").getSupportingIds());
            assertEquals(Set.of(), j.get("temp:public").getSupportingIds());
            assertEquals(Set.of("authorization"), j.get("temp:authorized").getSupportingIds());
            assertEquals(Set.of("places"), j.get("temp:authorization").getSupportingIds());
            assertEquals(Set.of(), j.get("temp:places").getSupportingIds());
        }
    }

    @Nested
    @DisplayName("Refine 'final'")
    class RefineFinalTest {
        @Test
        public void containsTheRightModels() {
            assertTrue(unit.exists("final"));
        }

        @Test
        public void modelsHaveTheRightType() {
            assertDoesNotThrow(() -> (Justification) unit.get("final"));
        }

        @Test
        public void patternContainsRightElements() {
            Counter counter = new Counter();
            unit.get("final").accept(counter);
            assertEquals(7, counter.getAccumulator().getOrDefault(Counter.Element.EVIDENCE,0));
            assertEquals(7, counter.getAccumulator().getOrDefault(Counter.Element.STRATEGY,0));
            assertEquals(6, counter.getAccumulator().getOrDefault(Counter.Element.SUB_CONCLUSION,0));
            assertEquals(1, counter.getAccumulator().getOrDefault(Counter.Element.CONCLUSION,0));
        }

        @Test
        public void elementsCanBeAccessed() {
            JustificationModel j = unit.get("final");
            assertEquals(j.get("c"),   j.get("final:c"));
            assertEquals(j.get("AND"), j.get("final:AND"));
            assertEquals(j.get("c1"), j.get("final:c1"));
            assertEquals(j.get("c2"), j.get("final:c2"));
            assertEquals(j.get("c3"), j.get("final:c3"));
            assertEquals(j.get("calling"), j.get("final:calling"));
            assertEquals(j.get("script"), j.get("final:script"));
            assertEquals(j.get("display"), j.get("final:display"));
            assertEquals(j.get("poster"), j.get("final:poster"));
            assertEquals(j.get("chat"), j.get("final:chat"));
            assertEquals(j.get("talking_points"), j.get("final:talking_points"));
            assertEquals(j.get("loc_available"), j.get("final:loc_available"));
            assertEquals(j.get("combining"), j.get("final:combining"));
            assertEquals(j.get("public"), j.get("final:public"));
            assertEquals(j.get("authorized"), j.get("final:authorized"));
            assertEquals(j.get("authorization"), j.get("final:authorization"));
            assertEquals(j.get("places"), j.get("final:places"));
            assertEquals(j.get("reused"), j.get("final:reused"));
            assertEquals(j.get("check"), j.get("final:check"));
            assertEquals(j.get("directory"), j.get("final:directory"));
            assertEquals(j.get("tou"), j.get("final:tou"));
        }

        @Test
        public void elementsHaveTheRightType() {
            JustificationModel j = unit.get("final");

            assertInstanceOf(Conclusion.class, j.get("final:c"));
            assertInstanceOf(Strategy.class, j.get("final:calling"));
            assertInstanceOf(SubConclusion.class, j.get("final:c1"));
            assertInstanceOf(SubConclusion.class, j.get("final:c2"));
            assertInstanceOf(SubConclusion.class, j.get("final:c3"));
            assertInstanceOf(SubConclusion.class, j.get("final:loc_available"));
            assertInstanceOf(SubConclusion.class, j.get("final:authorized"));
            assertInstanceOf(SubConclusion.class, j.get("final:reused"));
            assertInstanceOf(Strategy.class, j.get("final:calling"));
            assertInstanceOf(Strategy.class, j.get("final:display"));
            assertInstanceOf(Strategy.class, j.get("final:chat"));
            assertInstanceOf(Strategy.class, j.get("final:combining"));
            assertInstanceOf(Strategy.class, j.get("final:authorization"));
            assertInstanceOf(Strategy.class, j.get("final:check"));
            assertInstanceOf(Evidence.class, j.get("final:script"));
            assertInstanceOf(Evidence.class, j.get("final:directory"));
            assertInstanceOf(Evidence.class, j.get("final:poster"));
            assertInstanceOf(Evidence.class, j.get("final:talking_points"));
            assertInstanceOf(Evidence.class, j.get("final:public"));
            assertInstanceOf(Evidence.class, j.get("final:places"));
            assertInstanceOf(Evidence.class, j.get("final:tou"));
        }

        @Test
        public void elementsAreCorrectlySupported() {
            JustificationModel j = unit.get("final");
            assertEquals(Set.of("AND"),   j.get("final:c").getSupportingIds());
            assertEquals(Set.of("c1", "c2", "c3"), j.get("final:AND").getSupportingIds());
            assertEquals(Set.of("calling"), j.get("final:c1").getSupportingIds());
            assertEquals(Set.of("display"), j.get("final:c2").getSupportingIds());
            assertEquals(Set.of("chat"), j.get("final:c3").getSupportingIds());
            assertEquals(Set.of("script", "reused"), j.get("final:calling").getSupportingIds());
            assertEquals(Set.of(), j.get("final:script").getSupportingIds());
            assertEquals(Set.of("loc_available", "poster"), j.get("final:display").getSupportingIds());
            assertEquals(Set.of(), j.get("final:poster").getSupportingIds());
            assertEquals(Set.of("talking_points", "loc_available"), j.get("final:chat").getSupportingIds());
            assertEquals(Set.of(), j.get("final:talking_points").getSupportingIds());
            assertEquals(Set.of("combining"), j.get("final:loc_available").getSupportingIds());
            assertEquals(Set.of("public", "authorized"), j.get("final:combining").getSupportingIds());
            assertEquals(Set.of(), j.get("final:public").getSupportingIds());
            assertEquals(Set.of("authorization"), j.get("final:authorized").getSupportingIds());
            assertEquals(Set.of("places"), j.get("final:authorization").getSupportingIds());
            assertEquals(Set.of(), j.get("final:places").getSupportingIds());
            assertEquals(Set.of("check"), j.get("final:reused").getSupportingIds());
            assertEquals(Set.of("directory", "tou"), j.get("final:check").getSupportingIds());
            assertEquals(Set.of(), j.get("final:directory").getSupportingIds());
            assertEquals(Set.of(), j.get("final:tou").getSupportingIds());

        }
    }



}
