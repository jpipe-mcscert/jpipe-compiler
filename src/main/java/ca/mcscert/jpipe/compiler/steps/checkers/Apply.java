package ca.mcscert.jpipe.compiler.steps.checkers;

import ca.mcscert.jpipe.compiler.model.Checker;
import java.util.function.Consumer;

/**
 * Step used for debugging purpose to run an arbitrary function on a model, without changing it.
 * Often used for debugging purpose.
 *
 * @param <T> the input type of the function
 */
public class Apply<T> extends Checker<T> {

    private final Consumer<T> func;

    public Apply(Consumer<T> fun) {
        this.func = fun;
    }

    @Override
    protected void check(T input, String source) {
        func.accept(input);
    }
}
