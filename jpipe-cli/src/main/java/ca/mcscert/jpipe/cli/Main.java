package ca.mcscert.jpipe.cli;

import ca.mcscert.jpipe.compiler.Compiler;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "jpipe", description = "Compile and process jPipe justification files.", mixinStandardHelpOptions = true, version = "jPipe 2.0.0")
public class Main implements Callable<Integer> {

	@Parameters(index = "0", description = "The .jpipe source file to compile.")
	private File source;

	@Override
	public Integer call() throws Exception {
		new Compiler().compile(source.toPath());
		return 0;
	}

	public static void main(String[] args) {
		System.exit(new CommandLine(new Main()).execute(args));
	}
}
