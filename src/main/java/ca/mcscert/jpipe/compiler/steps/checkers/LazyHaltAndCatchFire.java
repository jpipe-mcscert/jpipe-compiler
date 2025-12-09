package ca.mcscert.jpipe.compiler.steps.checkers;

import ca.mcscert.jpipe.compiler.model.Checker;
import ca.mcscert.jpipe.error.ErrorManager;
import ca.mcscert.jpipe.error.FatalException;
import java.util.List;

/**
 * This checker is part of the "late error management" design choice of jPipe. A syntax error should
 * not prevent trying to render a model as an image, so that user can see visually where the error
 * is. As a consequence, one must collect errors, but let the process continue. With this checker,
 * we take a list of errors encountered in the previous steps. Is the list is not empty, we register
 * that the compilation will eventually fail, but let it continue to the next steps. It allows one
 * to render a model as an image even is the input file contains syntax errors.
 *
 * <p>
 *     The name is a reference to the HCF instruction in IBM System/360 (as well as good TV show).
 * </p>
 *
 * @param <T> type of model used at that point (only useful for chaining purpose).
 */
public final class LazyHaltAndCatchFire<T> extends Checker<T> {

    private final List<Throwable> errors;

    /**
     * Instantiate the HCF checker with the list of errors to check.
     *
     * @param errors a List of Throwable to check.
     */
    public LazyHaltAndCatchFire(List<Throwable> errors) {
        this.errors = errors;
    }

    @Override
    protected void check(T input, String source) {
        if (! this.errors.isEmpty()) {
            errors.forEach(ErrorManager.getInstance()::registerError);
            throw new FatalException("Compilation aborted");
        }
    }
}
