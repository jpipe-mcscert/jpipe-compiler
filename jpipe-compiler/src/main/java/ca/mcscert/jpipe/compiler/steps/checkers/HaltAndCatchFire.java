package ca.mcscert.jpipe.compiler.steps.checkers;

import ca.mcscert.jpipe.compiler.model.Checker;
import ca.mcscert.jpipe.compiler.model.CompilationContext;

/**
 * Converts accumulated non-fatal errors into a fatal signal, aborting the
 * pipeline at the next step boundary.
 *
 * <p>
 * This checker is part of the "late error management" design: lexing and
 * parsing report errors as non-fatal diagnostics so that both steps always run.
 * Placing this checker after them causes the pipeline to abort — via the
 * {@code ctx.hasFatalErrors()} fast-fail in
 * {@link ca.mcscert.jpipe.compiler.model.Transformation#fire} — before
 * downstream steps attempt to build or render a model from a broken parse tree.
 *
 * <p>
 * The name is a reference to the HCF instruction in IBM System/360 (as well as
 * a good TV show).
 * </p>
 *
 * @param <T>
 *            type of model used at that point (only useful for chaining
 *            purpose).
 */
public final class HaltAndCatchFire<T> extends Checker<T> {

	@Override
	protected void check(T input, CompilationContext ctx) {
		if (ctx.hasErrors()) {
			ctx.fatal("Compilation aborted due to syntax errors");
		}
	}
}
