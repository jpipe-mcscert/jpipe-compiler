package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.error.DuplicateSymbol;
import ca.mcscert.jpipe.error.UnknownSymbol;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SymbolTableTest {

    private SymbolTable<Integer> t123;
    private SymbolTable<Integer> t456;
    private SymbolTable<Integer> t34;

    @BeforeEach
    public void initialize() {
        t123 = new SymbolTable<>();
        t123.record("one", 1);
        t123.record("two", 2);
        t123.record("three", 3);

        t456 = new SymbolTable<>();
        t456.record("four", 4);
        t456.record("five", 5);
        t456.record("six", 6);

        t34 = new SymbolTable<>();
        t34.record("three", 3);
        t34.record("four", 4);

    }

    @Test
    public void scalarTable() {
        assertEquals(t123.get("one"), 1);
        assertThrows(UnknownSymbol.class, () -> t123.get("four"));
    }

    @Test
    public void cannotRecordDuplicates() {
        assertThrows(DuplicateSymbol.class, () -> t123.record("one", 42));
    }


}