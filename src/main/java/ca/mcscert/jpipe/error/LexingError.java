package ca.mcscert.jpipe.error;

/**
 * custom type to identify compilation error identified while running the compiler.
 */
@JPipeError
public class LexingError extends RuntimeException {

    private final String file;
    private final int line;
    private final int column;

    /**
     * constructor for a compilation error.
     *
     * @param l the line where the error was identified.
     * @param c the column where the error was identified.
     * @param message the compilation error message
     */
    public LexingError(String file, int l, int c, String message) {
        super("[" + file + ":" + l +  ":" + c + "] " + message);
        this.file = file;
        this.column = c;
        this.line = l;
    }

    @Override
    public String toString() {
        return super.getMessage();
    }
}
