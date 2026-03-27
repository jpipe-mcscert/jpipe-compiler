package ca.mcscert.jpipe.compiler.steps.io.sources;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.model.Source;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Pipeline source that opens a file and provides it as an {@link InputStream}.
 * Passing {@link CompilationConfig#STDIN} reads from standard input instead.
 */
public final class FileSource extends Source<InputStream> {

	@Override
	public InputStream provideFrom(String sourceName) throws IOException {
		if (sourceName.equals(CompilationConfig.STDIN)) {
			return System.in;
		}
		return new FileInputStream(sourceName);
	}
}
