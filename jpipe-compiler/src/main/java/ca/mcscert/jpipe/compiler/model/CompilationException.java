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
		super("Compilation failed in step [" + step + "]: "
				+ cause.getMessage(), cause);
	}

	public CompilationException(String step, String reason) {
		super("Compilation failed in step [" + step + "]: " + reason);
	}

}
