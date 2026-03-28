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
	 * Write {@code output} to the sink's pre-configured destination.
	 *
	 * @param output
	 *            the value produced by the preceding transformation chain.
	 * @throws IOException
	 *             if the destination cannot be written.
	 */
	void pourInto(O output) throws IOException;

}
