package ca.mcscert.jpipe.model;

import ca.mcscert.exemplars.SourceFileTest;
import ca.mcscert.jpipe.error.DuplicateSymbol;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RepTableTest {

    private RepTable<JustificationElement> table;
    private Evidence e1;
    private Evidence e2;
    private Evidence e3;

    @BeforeEach
    public void setUp() {
        e1 = new Evidence("id1", "label caption of e1");
        e2 = new Evidence("id2", "label caption of e2");
        e3 = new Evidence("id3", "label caption of e3");

        table = new RepTable<>();
        table.record(e1);
        table.record(e2,e1);
        table.record(e3,e2);
    }

    @Test
    public void findParentValue(){
        JustificationElement parent_element_e1 = table.getParent(e1);
        JustificationElement parent_element_e2 = table.getOriginalParent(e2);
        JustificationElement parent_element_e3 = table.getOriginalParent(e3);
        assertNull(parent_element_e1);
        assertEquals(parent_element_e2, e1);
        assertEquals(parent_element_e2, parent_element_e3);
    }

    @Test
    public void cannotRecordDuplicates(){
        assertThrows(DuplicateSymbol.class, () -> table.record(e1));
    }

    @Test
    public void deletingElement(){
        table.delete(e1);
        assertFalse(table.containsKey(e1));
        assertNull(table.getParent(e2));
    }


}
