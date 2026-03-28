package ca.mcscert.jpipe.compiler.model;

import java.io.IOException;

/**
 * First step of a compilation chain: reads an input file and produces the
 * initial value fed to the first {@link Transformation}.
 *
 * @param <I>
 *            type produced for the downstream transformation.
 */
public abstract class Source<I> {

	/** Functional interface for the {@link #of} factory. */
	@FunctionalInterface
	public interface Provider<I> {
		I provide(String sourceName) throws IOException;
	}

	/** Create a {@code Source} from a lambda or method reference. */
	public static <I> Source<I> of(Provider<I> provider) {
		return new Source<>() {
			@Override
			public I provideFrom(String sourceName) throws IOException {
				return provider.provide(sourceName);
			}
		};
	}

	/**
	 * Read {@code sourceName} and produce the initial pipeline value.
	 *
	 * @param sourceName
	 *            path to the input file.
	 * @return an instance of {@code I}.
	 * @throws IOException
	 *             if the file cannot be read.
	 */
	public abstract I provideFrom(String sourceName) throws IOException;

	/**
	 * DSL entry-point: combine this source with the first transformation to
	 * start building a compilation chain.
	 *
	 * @param next
	 *            the transformation to apply to this source's output.
	 * @param <R>
	 *            output type of {@code next}.
	 * @return a {@link ChainBuilder} accumulating further steps.
	 */
	public final <R> ChainBuilder<I, R> andThen(Transformation<I, R> next) {
		return new ChainBuilder<>(this, next);
	}

}
