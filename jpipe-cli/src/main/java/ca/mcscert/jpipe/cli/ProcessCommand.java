package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.compiler.Format;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

/**
 * Compiles a {@code .jd} source file and exports the selected model in the
 * requested format.
 */
@Command(name = "process", description = "Compile and export a jPipe model.", mixinStandardHelpOptions = true)
class ProcessCommand implements Callable<Integer> {

	@ParentCommand
	private Main parent;

	@Option(names = {"-i",
			"--input"}, description = "Input .jd source file (default: stdin).", defaultValue = CompilationConfig.STDIN)
	private String input;

	@Option(names = {"-o",
			"--output"}, description = "Output file (default: stdout).", defaultValue = CompilationConfig.STDOUT)
	private String output;

	@Option(names = {"-f",
			"--format"}, description = "Output format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).", defaultValue = "JPIPE")
	private Format format;

	@Option(names = {"-d",
			"--diagram"}, description = "Name of the model to export.", required = true)
	private String diagram;

	@Override
	public Integer call() {
		if (!parent.headless) {
			Logo.sout();
		}
		try (FileOutputStream fileOut = output.equals(CompilationConfig.STDOUT)
				? null
				: new FileOutputStream(output)) {
			OutputStream out = fileOut != null ? fileOut : System.out;
			CompilationConfig config = new CompilationConfig(input, output,
					format, diagram);
			boolean hasErrors = CompilerFactory.build(config, out)
					.compile(input, output);
			return hasErrors ? Main.EXIT_JPIPE_ERROR : Main.EXIT_OK;
		} catch (CompilationException | UnsupportedOperationException e) {
			System.err.println("error: " + e.getMessage());
			return Main.EXIT_JPIPE_ERROR;
		} catch (Exception e) {
			System.err.println("unexpected error: " + e.getMessage());
			return Main.EXIT_SYSTEM_ERROR;
		}
	}
}
