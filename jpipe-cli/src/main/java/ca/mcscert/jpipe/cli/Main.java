package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.compiler.Format;
import ca.mcscert.jpipe.compiler.Mode;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "jpipe", description = "Compile and process jPipe justification files.", mixinStandardHelpOptions = true, version = "jPipe 2.0.0")
public class Main implements Callable<Integer> {

	static final int EXIT_OK = 0;
	static final int EXIT_JPIPE_ERROR = 1;
	static final int EXIT_SYSTEM_ERROR = 42;

	@Option(names = {"-i",
			"--input"}, description = "Input .jd source file (default: stdin).", defaultValue = CompilationConfig.STDIN)
	private String input;

	@Option(names = {"-o",
			"--output"}, description = "Output file (default: stdout).", defaultValue = CompilationConfig.STDOUT)
	private String output;

	@Option(names = {
			"--mode"}, description = "Compilation mode: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).", defaultValue = "PROCESS")
	private Mode mode;

	@Option(names = {"-f",
			"--format"}, description = "Output format: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).", defaultValue = "JPIPE")
	private Format format;

	@Option(names = {"--headless"}, description = "Suppress logo output.")
	private boolean headless;

	@Option(names = {"-d",
			"--diagram"}, description = "Name of the model to export (required when the source defines multiple models).")
	private String diagram;

	@Option(names = {
			"--doctor"}, description = "Check that required external tools are available on PATH.")
	private boolean doctor;

	@Override
	public Integer call() {
		if (!headless) {
			Logo.sout();
		}
		if (doctor) {
			System.out.println("Checking external tools:");
			return Doctor.run() ? EXIT_OK : EXIT_JPIPE_ERROR;
		}
		try {
			OutputStream out = output.equals(CompilationConfig.STDOUT)
					? System.out
					: new FileOutputStream(output);
			CompilationConfig config = new CompilationConfig(input, output,
					mode, format, diagram);
			CompilerFactory.build(config, out).compile(input, output);
			return EXIT_OK;
		} catch (CompilationException | UnsupportedOperationException e) {
			System.err.println("error: " + e.getMessage());
			return EXIT_JPIPE_ERROR;
		} catch (Exception e) {
			System.err.println("unexpected error: " + e.getMessage());
			return EXIT_SYSTEM_ERROR;
		}
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new Main())
				.setCaseInsensitiveEnumValuesAllowed(true).execute(args));
	}
}
