package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.compiler.Format;
import ca.mcscert.jpipe.compiler.Mode;
import java.util.concurrent.Callable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "jpipe", description = "Compile and process jPipe justification files.", mixinStandardHelpOptions = true, version = "jPipe 2.0.0")
public class Main implements Callable<Integer> {

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

	@Override
	public Integer call() throws Exception {
		if (!headless) {
			Logo.sout();
		}
		CompilationConfig config = new CompilationConfig(input, output, mode, format);
		CompilerFactory.build(config).compile(input, output);
		return 0;
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new Main()).execute(args));
	}
}
