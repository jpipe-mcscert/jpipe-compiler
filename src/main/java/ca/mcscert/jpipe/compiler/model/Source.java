package ca.mcscert.jpipe.compiler.model;

import java.io.IOException;

/**
 * Model a Source for a compilation chain, i.e., the first step.
 *
 * @param <I> the input type it produces for the upcoming transformation.
 */
public abstract class Source<I> {

    /**
     * Consume a file to produce the input data fed to the next transformation in chain.
     *
     * @param sourceName path to the input file.
     * @return an Instance of I produced out of sourceName
     * @throws IOException is something goes wrong.
     */
    public abstract I provideFrom(String sourceName) throws IOException;

    /**
     * Syntactic sugar used as internal DSL to kickstart the definition of a Compilation Chain. We
     * use a "builder" pattern to transform a source and a transformation into a ChainBuilder. The
     * builder will accumulate transformation, until a Sink is provided, finalizing a ChainCompiler.
     *
     * @param next the transformation to execute on the output of this Source
     * @param <R> the output produced by the transformation, for chaining purposes.
     * @return a ChainBuilder (aka incomplete compiler)
     */
    public final <R> ChainBuilder<I, R> andThen(Transformation<I, R> next) {
        return new ChainBuilder<>(this, next);
    }

}
