package ca.mcscert.jpipe.error;

/**
 * Error thrown when a duplicate symbol is found in a scope.
 */
@JPipeError
public class DuplicateSymbol extends RuntimeException {

    public DuplicateSymbol(String symbol) {
        super("Duplicated symbol [" + symbol + "]");
    }

}
