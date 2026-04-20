package ca.mcscert.jpipe.compiler.model;

/**
 * Unchecked exception thrown when a compilation step fails unexpectedly.
 * Checked exceptions thrown inside {@link Transformation#run} are wrapped here
 * so the pipeline can propagate them without polluting every call-site.
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
