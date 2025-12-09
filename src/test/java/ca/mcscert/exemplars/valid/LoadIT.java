package ca.mcscert.exemplars.valid;

import ca.mcscert.exemplars.Counter;
import ca.mcscert.exemplars.SourceFileTest;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Source file: src/test/resources/valid/load.jd")
public class LoadIT extends SourceFileTest {

    @Override
    protected String source() {
        return "valid/load.jd";
    }

    @Test
    public void containsLoadedJustifications() {
        Counter counter = new Counter();
        this.unit.accept(counter);
        System.out.println(unit);
        assertEquals(4, unit.getContents().size());
        assertTrue(unit.exists("proving"));
        assertTrue(unit.exists("prover"));
        assertTrue(unit.exists("partial"));
        assertTrue(unit.exists("prove_models"));
    }
    

}
