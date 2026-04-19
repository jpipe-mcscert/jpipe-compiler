package ca.mcscert.jpipe.compiler.model;

import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A single step in a compilation chain: a function {@code I → O}.
 *
 * <p>
 * Concrete steps implement {@link #run}. Callers always go through
 * {@link #fire}, which adds logging, null-output detection, fast-fail on
 * accumulated fatal errors, and uniform error wrapping. Steps are composed via
 * {@link #andThen}.
 *
 * @param <I>
 *            input type.
 * @param <O>
 *            output type.
 */
public abstract class Transformation<I, O> {

	protected static final Logger logger = LogManager
			.getLogger(Transformation.class);

	/**
	 * Functional interface for the {@link #of} factory — allows steps to be
	 * expressed as lambdas or method references.
	 */
	@FunctionalInterface
	public interface Step<I, O> {
		@SuppressWarnings("java:S112")
		O apply(I input, CompilationContext ctx) throws Exception;
	}

	/**
	 * Create a {@code Transformation} from a lambda or method reference.
	 *
	 * @param step
	 *            the lambda implementing the step logic.
	 * @param <I>
	 *            input type.
	 * @param <O>
	 *            output type.
	 * @return a {@code Transformation} that delegates {@link #run} to
	 *         {@code step}.
	 */
	public static <I, O> Transformation<I, O> of(Step<I, O> step) {
		return new Transformation<>() {
			@Override
			protected O run(I input, CompilationContext ctx) throws Exception {
				return step.apply(input, ctx);
			}

			@Override
			protected boolean shouldLog() {
				return false;
			}
		};
	}

	/**
	 * Human-readable name used in diagnostic messages. Named subclasses inherit
	 * the default (their simple class name). Composed transformations produced
	 * by {@link #andThen} override this to propagate the name of the leftmost
	 * step so that "pipeline aborted" messages point to the first step that
	 * would have done work.
	 */
	protected String stepName() {
		return getClass().getSimpleName();
	}

	/**
	 * Whether {@link #fire} should emit a DEBUG log entry for this step.
	 * Returns {@code true} for all named (concrete) steps. Overridden to
	 * {@code false} in the anonymous composition wrappers produced by
	 * {@link #andThen}, so that only real pipeline steps appear in the log —
	 * not the N intermediate wrappers created by composition.
	 */
	protected boolean shouldLog() {
		return true;
	}

	/**
	 * Business logic of this step. Must not return {@code null}; may throw any
	 * exception. Use {@code ctx} to report non-fatal diagnostics or inspect
	 * previously accumulated errors.
	 *
	 * @param input
	 *            the value produced by the previous step.
	 * @param ctx
	 *            compilation context carrying the source path and diagnostic
	 *            bag.
	 * @return the transformed value — never {@code null}.
	 * @throws Exception
	 *             if anything goes wrong.
	 */
	@SuppressWarnings("java:S112")
	protected abstract O run(I input, CompilationContext ctx) throws Exception;

	/**
	 * Public entry-point: runs this step with logging and error handling.
	 *
	 * <ul>
	 * <li>Fast-fails if the context already holds fatal errors (a previous step
	 * marked the pipeline as broken).</li>
	 * <li>Wraps any checked exception in {@link CompilationException}.</li>
	 * <li>Rejects a {@code null} return from {@link #run} as a programming
	 * error in the step implementation.</li>
	 * </ul>
	 *
	 * @param in
	 *            the input value.
	 * @param ctx
	 *            compilation context.
	 * @return the output value — never {@code null}.
	 */
	public final O fire(I in, CompilationContext ctx) {
		if (ctx.hasFatalErrors()) {
			throw new CompilationException(stepName(),
					"pipeline aborted due to previous fatal errors");
		}
		if (shouldLog()) {
			String name = stepName();
			logger.debug("Firing transformation [{}]", name);
		}
		try {
			O result = run(in, ctx);
			return Objects.requireNonNull(result,
					"Transformation [" + stepName() + "] returned null");
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new CompilationException(stepName(), e);
		}
	}

	/**
	 * Compose this transformation with {@code next}, producing a new
	 * transformation that runs both in sequence.
	 *
	 * <pre>
	 *   this : I → O
	 *   next : O → R
	 *   this.andThen(next) : I → R
	 * </pre>
	 *
	 * @param next
	 *            transformation to apply after this one.
	 * @param <R>
	 *            output type of the composed transformation.
	 * @return a new anonymous transformation implementing {@code next ∘ this}.
	 */
	public final <R> Transformation<I, R> andThen(Transformation<O, R> next) {
		Transformation<I, O> self = this;
		return new Transformation<>() {
			@Override
			protected R run(I input, CompilationContext ctx) throws Exception {
				return next.fire(self.fire(input, ctx), ctx);
			}

			@Override
			protected String stepName() {
				return self.stepName();
			}

			@Override
			protected boolean shouldLog() {
				return false;
			}
		};
	}

}
