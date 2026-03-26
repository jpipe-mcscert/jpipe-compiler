package ca.mcscert.jpipe.compiler.model;

/**
 * An idempotent pipeline step: inspects the value without modifying it.
 *
 * <p>
 * Subclasses implement {@link #check}. The {@link #run} implementation
 * delegates to {@code check} and returns the input unchanged, enforcing
 * idempotency by construction.
 *
 * <p>
 * Checks may report non-fatal diagnostics via {@code ctx} rather than throwing,
 * allowing the pipeline to accumulate multiple errors before deciding whether
 * to abort.
 *
 * @param <I>
 *            the type being checked.
 */
public abstract class Checker<I> extends Transformation<I, I> {

	/** Functional interface for the {@link #of} factory. */
	@FunctionalInterface
	public interface Check<I> {
		void apply(I input, CompilationContext ctx) throws Exception;
	}

	/** Create a {@code Checker} from a lambda or method reference. */
	public static <I> Checker<I> checking(Check<I> check) {
		return new Checker<>() {
			@Override
			protected void check(I input, CompilationContext ctx) throws Exception {
				check.apply(input, ctx);
			}
		};
	}

	@Override
	protected final I run(I input, CompilationContext ctx) throws Exception {
		check(input, ctx);
		return input;
	}

	/**
	 * Perform the check. Must not modify {@code input}. Non-fatal issues should be
	 * reported via {@code ctx.error()} or {@code ctx.warn()} rather than thrown;
	 * throw (or call {@code ctx.fatal()}) only for unrecoverable failures.
	 *
	 * @param input
	 *            the value to inspect.
	 * @param ctx
	 *            compilation context for reporting diagnostics.
	 * @throws Exception
	 *             if the check encounters an unrecoverable failure.
	 */
	protected abstract void check(I input, CompilationContext ctx) throws Exception;

}
