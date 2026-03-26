package ca.mcscert.jpipe.compiler;

import java.io.IOException;

/**
 * Placeholder compiler used until the full pipeline is wired up.
 */
public final class StubCompiler implements Compiler {

	@Override
	public void compile(String sourceFile, String sinkFile) throws IOException {
		throw new UnsupportedOperationException("Not implemented yet");
	}

}
