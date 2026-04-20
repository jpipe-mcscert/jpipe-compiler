package ca.mcscert.jpipe.operators;

/**
 * Thrown by {@link CompositionOperator#apply} when one or more required
 * argument keys are missing from the provided argument map.
 *
 * <p>
 * Unchecked, consistent with the existing {@code DeadlockException} and
 * {@code CompilationException} conventions in jpipe-model.
 */
public final class InvalidOperatorCallException extends RuntimeException {

	public InvalidOperatorCallException(String message) {
		super(message);
	}
}
