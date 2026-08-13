package ca.mcscert.jpipe.operators;

/**
 * Raised when unification groups elements whose kinds cannot be merged into a
 * single element, e.g. a strategy and an evidence sharing the same label.
 *
 * <p>
 * Reported independently of the order in which the source models were listed:
 * the operator is commutative, so the same group is rejected, with the same
 * message, whichever way its members were written.
 */
public final class IncompatibleUnificationException extends RuntimeException {

	public IncompatibleUnificationException(String message) {
		super(message);
	}
}
