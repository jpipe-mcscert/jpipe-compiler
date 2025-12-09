package ca.mcscert.exemplars.examples;

import ca.mcscert.exemplars.Counter;
import ca.mcscert.exemplars.SourceFileTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Source file: src/test/resources/examples/recruitement.jd")
public class recruitmentIT extends SourceFileTest {
    @Override
    protected String source() {
        return "examples/recruitement.jd";
    }

    @Test
    public void containsOneJustification() {
        Counter counter = new Counter();
        this.unit.accept(counter);
        assertEquals(11, unit.getContents().size());
    }


}
