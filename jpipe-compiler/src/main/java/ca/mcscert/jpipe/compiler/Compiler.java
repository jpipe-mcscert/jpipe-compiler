package ca.mcscert.jpipe.compiler;

import java.io.IOException;

/**
 * Contract for a compiler: consume an input file and produce an output file.
 */
public interface Compiler {

	/**
	 * Trigger the compilation process.
	 *
	 * @param sourceFile
	 *            input file path.
	 * @param sinkFile
	 *            output file path.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	void compile(String sourceFile, String sinkFile) throws IOException;

}
