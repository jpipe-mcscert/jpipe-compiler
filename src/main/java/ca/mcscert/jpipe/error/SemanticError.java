package ca.mcscert.jpipe.error;

/**
 * Identify a semantic error that prevent the compilation of the provided model.
 */
@JPipeError
public class SemanticError extends RuntimeException {

    /**
     * We instantiate the error with a "meaningful" message.
     *
     * @param message the message describing the root cause of the error.
     */
    public SemanticError(String message) {
        super(message);
    }

}
