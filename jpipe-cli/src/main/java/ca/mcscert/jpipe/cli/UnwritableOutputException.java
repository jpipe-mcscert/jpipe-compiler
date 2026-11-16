package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import java.io.IOException;

/**
 * The output of a command could not be written: its file, or standard output.
 * Its message names the target and the reason, ready to be shown to the user as
 * is.
 */
final class UnwritableOutputException extends IOException {

	UnwritableOutputException(String target, String reason) {
		super(describe(target) + ": " + reason);
	}

	UnwritableOutputException(String target, String reason, Throwable cause) {
		super(describe(target) + ": " + reason, cause);
	}

	private static String describe(String target) {
		return CompilationConfig.STDOUT.equals(target)
				? "cannot write to standard output"
				: "cannot write '" + target + "'";
	}
}
