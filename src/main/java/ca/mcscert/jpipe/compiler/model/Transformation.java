package ca.mcscert.jpipe.compiler.model;


import ca.mcscert.jpipe.error.ErrorManager;
import ca.mcscert.jpipe.error.FatalException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Implements a transformation as part of a compilation chain.
 * One can see a transformation as function t: I -> O.
 *
 * <p>
 *     We cannot use the "Function" interface from Java as we might need to configure the function
 *     with additional parameter coming from CLI configuration, so we need more control on its
 *     instantiation process and error handling.
 * </p>
 *
 * @param <I> The input type of the transformation.
 * @param <O> The output type of the transformation.
 */
public abstract class Transformation<I, O> {

    protected static final Logger logger = LogManager.getLogger();

    /**
     * A transformation implements its business logic in this method (template method pattern).
     *
     * @param input the input element
     * @param source the source file containing the element.
     * @return an instance of O as the result of processing input.
     * @throws Exception is anything goes wrong.
     */
    protected abstract O run(I input, String source) throws Exception;

    /**
     * Public interface to execute a transformation, taking care of proper error handling.
     *
     * @param in the input element.
     * @param source the source file location (to record error messages).
     * @return the expected output
     */
    public final O fire(I in, String source) {
        if (this.getClass().getCanonicalName() != null) {
            logger.info("Firing transformation [{}]", this.getClass().getCanonicalName());
        }
        O out = null;
        try {
            out = run(in, source);
            return out;
        } catch (FatalException fe) {
            throw fe; // propagate the exception to stop the process
        } catch (Exception e) {
            // Something unexpected went wrong. Register and abort;
            ErrorManager.getInstance().fatal(e);
            return null;
        }
    }

    /**
     * Composes this with another transformation, to be executed after this one. We use a functional
     * approach and instantiates a new anonymous Transformation than implements next º this.
     *
     * <p>
     *     this: I --> O
     *           i |-> o = this(i)
     *     next: O --> R
     *           o |-> r = next(o)
     *     this.andThen(next): I --> R
     *                         i |-> r = next(this(i))
     * </p>
     *
     * @param next the next transformation, consuming an O as input (because this produces an O).
     * @param <R> the output type of the new transformation (next's output type).
     * @return an anonymous transformation implementing next º this.
     */
    public final <R> Transformation<I, R> andThen(Transformation<O, R> next) {
        Transformation<I, O> myself = this;
        return new Transformation<>() {
            @Override protected R run(I input, String source) throws Exception {
                return next.fire(myself.fire(input, source), source);
            }
        };
    }

}


