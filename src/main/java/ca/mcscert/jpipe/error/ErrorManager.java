package ca.mcscert.jpipe.error;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class to record errors encountered while compiling, exposed as a singleton.
 *
 * @author mosser
 */
public final class ErrorManager {

    private static final Logger logger = LogManager.getLogger();

    private static ErrorManager instance;

    private final List<Throwable> errors = new ArrayList<>();

    /**
     * private constructor to prevent accidental instantiation from outside.
     */
    private ErrorManager() {}

    /**
     * Access to the singleton-ed instance.
     *
     * @return the ErrorManager singleton.
     */
    public static ErrorManager getInstance() {
        if (instance == null) {
            instance = new ErrorManager();
        }
        return instance;
    }

    /**
     * Register a non-fatal error encountered when compiling.
     *
     * @param error the error/exception to record.
     */
    public void registerError(Throwable error) {
        logger.info("Registering recoverable error [{}]", error.toString());
        errors.add(error);
    }

    /**
     * Register a fatal error, and immediately stop the compiler as a side effect.
     *
     * @param error the error/exception to record as fatal
     */
    public void fatal(Throwable error) {
        logger.info("Registering fatal error [{}]", error.toString());
        errors.add(error);
        throw new FatalException();
    }

    /**
     * Flush the contents of the manager (i.e., the recorder errors).
     *
     * @return the concatenation of all encountered errors so far
     */
    @Override
    public String toString() {
        if (this.errors.isEmpty()) {
            return "";
        }
        StringJoiner buffer = new StringJoiner("\n", "", "");
        for (Throwable t : errors.reversed()) {
            buffer.add((isLanguageError(t) ? processLanguageError(t) : processSystemError(t)));
        }
        return buffer.add("").toString();
    }

    public int length() {
        return this.errors.size();
    }


    private StringBuffer processSystemError(Throwable t) {
        logger.debug("  Processing system error");
        StringBuffer buffer = new StringBuffer();
        buffer.append("[system] ")
                .append(t.getClass().getName())
                .append(" ").append(t.getMessage());
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        buffer.append(System.lineSeparator()).append(sw);
        return buffer;
    }

    private StringBuffer processLanguageError(Throwable t) {
        logger.debug("  Processing jPipe error");
        StringBuffer buffer = new StringBuffer();
        buffer.append("[error] ")
                .append(t.toString());
        return buffer;
    }

    private boolean isLanguageError(Throwable t) {
        return t.getClass().isAnnotationPresent(JPipeError.class);
    }



}
