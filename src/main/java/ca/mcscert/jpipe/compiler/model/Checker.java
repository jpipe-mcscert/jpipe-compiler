package ca.mcscert.jpipe.compiler.model;

/**
 * model what a checker is. Basically an idempotent transformation.
 * A checker does not "stop" the compilation if encountering errors. This is delegated to the
 * error management mechanism, as well as the "Halt and Catch Fire" transformation.
 *
 * @param <I> the element type consumed by the checker.
 */
public abstract class Checker<I> extends Transformation<I, I> {

    /**
     * Template method implementation to force idempotency.
     *
     * @param input input model (will be returned without being modified)
     * @param source location of the input model in the file system
     * @return input, with no side effect
     * @throws Exception is something goes wrong.
     */
    @Override
    protected final I run(I input, String source) throws Exception {
        check(input, source);
        return input;
    }

    /**
     * Method used to implement the concrete check.
     * The method must not have any side effect on input.
     *
     * @param input input model to check.
     * @param source file where the model was located (for error reporting).
     */
    protected abstract void check(final I input, String source);

}
