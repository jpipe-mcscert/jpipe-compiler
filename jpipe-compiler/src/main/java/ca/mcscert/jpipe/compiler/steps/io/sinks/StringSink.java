package ca.mcscert.jpipe.compiler.steps.io.sinks;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.model.Sink;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pipeline sink that writes a {@link String} to a file. Passing
 * {@link CompilationConfig#STDOUT} writes to the {@link OutputStream} supplied
 * at construction time instead of directly referencing {@code System.out}.
 */
public final class StringSink implements Sink<String> {

	private final PrintStream stdout;

	public StringSink(OutputStream stdout) {
		this.stdout = new PrintStream(stdout);
	}

	@Override
	public void pourInto(String output, String fileName) throws IOException {
		if (fileName.equals(CompilationConfig.STDOUT)) {
			stdout.print(output);
		} else {
			Files.writeString(Path.of(fileName), output);
		}
	}
}
