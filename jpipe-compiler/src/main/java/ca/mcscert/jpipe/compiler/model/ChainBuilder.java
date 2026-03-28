package ca.mcscert.jpipe.compiler.model;

/**
 * Fluent builder that accumulates {@link Transformation}s into a compilation
 * chain. Call {@link #andThen(Transformation)} to append steps; finalise with
 * {@link #andThen(Sink)} to produce a {@link ChainCompiler}.
 *
 * @param <I>
 *            input type of the chain (type produced by the {@link Source}).
 * @param <O>
 *            current output type (changes with each {@code andThen} call).
 */
public final class ChainBuilder<I, O> {

	private final Source<I> source;
	private final Transformation<I, O> chain;

	ChainBuilder(Source<I> source, Transformation<I, O> chain) {
		this.source = source;
		this.chain = chain;
	}

	/**
	 * Append a transformation to the end of the current chain.
	 *
	 * @param next
	 *            the step to add.
	 * @param <R>
	 *            new output type.
	 * @return a new builder with the extended chain.
	 */
	public <R> ChainBuilder<I, R> andThen(Transformation<O, R> next) {
		return new ChainBuilder<>(source, chain.andThen(next));
	}

	/**
	 * Finalise the chain by attaching a sink, producing a ready-to-use
	 * {@link Compiler}.
	 *
	 * @param sink
	 *            the final serialisation step.
	 * @return a complete {@link ChainCompiler}.
	 */
	public ChainCompiler<I, O> andThen(Sink<O> sink) {
		return new ChainCompiler<>(source, chain, sink);
	}

	/**
	 * Expose the accumulated chain as a plain {@link Transformation}, useful
	 * when the chain is embedded inside a larger pipeline rather than used
	 * standalone.
	 *
	 * @return the composed transformation.
	 */
	public Transformation<I, O> asTransformation() {
		return chain;
	}

}
