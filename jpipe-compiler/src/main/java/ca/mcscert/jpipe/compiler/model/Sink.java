package ca.mcscert.jpipe.compiler.model;

import java.io.IOException;

/**
 * Last step of a compilation chain: serialises the pipeline output to a file.
 *
 * @param <O>
 *            type of the value to serialise.
 */
public interface Sink<O> {

	/**
	 * Write {@code output} to {@code fileName}.
	 *
	 * @param output
	 *            the value produced by the preceding transformation chain.
	 * @param fileName
	 *            destination file path.
	 * @throws IOException
	 *             if the file cannot be written.
	 */
	void pourInto(O output, String fileName) throws IOException;

}
