package ca.mcscert.jpipe.compiler;

import java.io.IOException;

/**
 * Model what a compiler is.
 */
public interface Compiler {

    /**
     * Trigger the compilation process.
     *
     * @param sourceFile input file to be used.
     * @param sinkFile output file to be used.
     * @throws IOException is something goes wrong.
     */
    void compile(String sourceFile, String sinkFile) throws IOException;

}
