package ca.mcscert.jpipe.compiler.model;

/**
 * Builder pattern to be used as internal DSL when building a compilation chain.
 *
 * @param <I> Input type of the chain (usually InputStream)
 * @param <O> Output type of the chain (usually serializable format)
 */
public final class ChainBuilder<I, O> {

    private final Source<I> source;
    private final Transformation<I, O> chain;

    /**
     * Create a chain builder starting with source and applying next transformation.
     *
     * @param source the source to be used when initiating the compilation process.
     * @param transformation the transformation to apply.
     */
    public ChainBuilder(Source<I> source, Transformation<I, O> transformation) {
        this.chain = transformation;
        this.source = source;
    }

    /**
     * Chain a transformation to the one already stored in the chain (at the end).
     *
     * @param next the transformation to add after the one already stored.
     * @param <R> the new return type of the builder, thanks to the new transformation.
     * @return a new ChainBuilder, with the composed transformation inside it.
     */
    public <R> ChainBuilder<I, R> andThen(Transformation<O, R> next) {
        return new ChainBuilder<>(this.source, chain.andThen(next));
    }

    /**
     * Finalize the builder by adding a Sink as its final step (creating a Compiler).
     *
     * @param sink the sink to be used to finalize the compilation process
     * @return A Compiler (which cannot be modified anymore)
     */
    public ChainCompiler<I, O> andThen(Sink<O> sink) {
        return new ChainCompiler<>(this.source, this.chain, sink);
    }


    public Transformation<I, O> asTransformation() {
        return this.chain;
    }

}
