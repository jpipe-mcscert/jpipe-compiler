package ca.mcscert.jpipe.compiler.steps.io.sinks;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.model.Sink;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Pipeline sink that writes a {@link String} to a file. Passing
 * {@link CompilationConfig#STDOUT} writes to standard output instead.
 */
public final class StringSink implements Sink<String> {

	@Override
	public void pourInto(String output, String fileName) throws IOException {
		if (fileName.equals(CompilationConfig.STDOUT)) {
			System.out.print(output);
		} else {
			Files.writeString(Path.of(fileName), output);
		}
	}
}
