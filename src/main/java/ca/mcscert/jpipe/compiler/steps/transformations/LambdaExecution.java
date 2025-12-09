package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.Transformation;
import java.util.function.Function;

/**
 * Execute a provided lambda function.
 *
 * @param <I> the input type of the lambda function
 * @param <O> the output type of the lambda function
 */
public final class LambdaExecution<I, O> extends Transformation<I, O> {

    private final Function<I, O> lambda;

    public LambdaExecution(Function<I, O> lambda) {
        this.lambda = lambda;
    }

    @Override
    protected O run(I input, String source) throws Exception {
        return lambda.apply(input);
    }

}
