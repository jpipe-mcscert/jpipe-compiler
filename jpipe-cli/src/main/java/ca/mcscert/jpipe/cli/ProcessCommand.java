package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.compiler.Format;
import java.io.IOException;
import java.io.OutputStream;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Compiles a {@code .jd} source file and exports the selected model in the
 * requested format.
 */
@Command(name = "process", description = "Compile and export a jPipe model.", mixinStandardHelpOptions = true, versionProvider = Main.ManifestVersionProvider.class)
class ProcessCommand extends InputOutputCommand {

	@Option(names = {"-f",
			"--format"}, description = "Output format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).", defaultValue = "JPIPE")
	private Format format;

	@Option(names = {"-m",
			"--model"}, description = "Name of the model to export.", required = true)
	private String model;

	@Spec
	private CommandSpec spec;

	@Override
	protected Integer doCall(OutputStream out) throws IOException {
		CompilationConfig config = new CompilationConfig(input, output, format,
				model);
		// Diagnostics go to standard error through picocli's writer, not
		// through a logger: that is what lets the default run leave the
		// logging back end unstarted (#216).
		boolean hasErrors = CompilerFactory
				.build(config, out, spec.commandLine().getErr())
				.compile(input, output);
		return hasErrors ? Main.EXIT_JPIPE_ERROR : Main.EXIT_OK;
	}
}
