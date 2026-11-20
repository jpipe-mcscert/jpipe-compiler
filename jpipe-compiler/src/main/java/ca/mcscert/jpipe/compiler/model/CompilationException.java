package ca.mcscert.jpipe.compiler.model;

/**
 * Unchecked exception thrown when a compilation step cannot continue.
 *
 * <p>
 * {@link Transformation#fire} throws it to fast-fail once the context holds a
 * fatal error. A step throws it itself, wrapping any checked exception it
 * meets, since {@link Transformation#run} declares none. {@code fire} does no
 * wrapping: any other exception raised by {@code run} propagates unchanged.
 */
public class CompilationException extends RuntimeException {

	public CompilationException(String step, Throwable cause) {
		super(message(step, cause.getMessage()), cause);
	}

	public CompilationException(String step, String reason) {
		super(message(step, reason));
	}

	/**
	 * A step that could not continue because of {@code cause}, explained by
	 * {@code reason}. A caller can tell from the cause that the failure came
	 * from outside the compiler, such as an {@link java.io.IOException}.
	 */
	public CompilationException(String step, String reason, Throwable cause) {
		super(message(step, reason), cause);
	}

	private static String message(String step, String reason) {
		return "Compilation failed in step [" + step + "]: " + reason;
	}

}
