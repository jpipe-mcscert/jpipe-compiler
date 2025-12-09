package ca.mcscert.jpipe.error;

/**
 * Custom exception to model a symbol not found in the symbol table.
 */
@JPipeError
public class UnknownSymbol extends RuntimeException {

    public UnknownSymbol(String symbol, String location) {
        super("Unknown symbol [" + symbol + "] (location: " + location + ")");
    }

    public UnknownSymbol(String symbol) {
        super("Unknown symbol [" + symbol + "] (location: ?)");
    }

}
