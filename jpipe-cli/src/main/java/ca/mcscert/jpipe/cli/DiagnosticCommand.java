package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilerFactory;
import java.io.IOException;
import java.io.OutputStream;
import picocli.CommandLine.Command;

/**
 * Parses a {@code .jd} source file and prints a human-readable diagnostic
 * report without exporting any model.
 */
@Command(name = "diagnostic", description = "Parse and report diagnostics without exporting.", mixinStandardHelpOptions = true)
class DiagnosticCommand extends InputOutputCommand {

	@Override
	protected Integer doCall(OutputStream out) throws IOException {
		boolean hasErrors = CompilerFactory.buildDiagnosticCompiler(out)
				.compile(input, output);
		return hasErrors ? Main.EXIT_JPIPE_ERROR : Main.EXIT_OK;
	}
}
