package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.compiler.DiagnosticFormat;
import java.io.IOException;
import java.io.OutputStream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

/**
 * Parses a {@code .jd} source file and reports on it without exporting any
 * model, either as a human-readable report or as JSON for tooling.
 */
@Command(name = "diagnostic", description = "Parse and report diagnostics without exporting.", mixinStandardHelpOptions = true)
class DiagnosticCommand extends InputOutputCommand {

	/**
	 * Initialised as well as declared with a picocli default, so that the
	 * command behaves identically whether picocli populated it or a caller
	 * instantiated the command directly.
	 */
	@Option(names = {"-f",
			"--format"}, description = "Report format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).", defaultValue = "TEXT")
	protected DiagnosticFormat format = DiagnosticFormat.TEXT;

	@Override
	protected Integer doCall(OutputStream out) throws IOException {
		boolean hasErrors = CompilerFactory.buildDiagnosticCompiler(format, out)
				.compile(input, output);
		return hasErrors ? Main.EXIT_JPIPE_ERROR : Main.EXIT_OK;
	}

	/**
	 * The logo would sit in front of the JSON document and make it unparseable,
	 * so it is suppressed whenever a machine-readable report goes to standard
	 * output. Writing to a file keeps the banner: the two streams are separate.
	 */
	@Override
	protected boolean suppressLogo() {
		return format == DiagnosticFormat.JSON
				&& CompilationConfig.STDOUT.equals(output);
	}
}
