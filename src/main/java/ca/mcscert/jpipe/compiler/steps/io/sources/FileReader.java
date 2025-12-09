package ca.mcscert.jpipe.compiler.steps.io.sources;

import ca.mcscert.jpipe.cli.Configuration;
import ca.mcscert.jpipe.compiler.model.Source;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Compiler source, providing an InputStream from a File.
 */
public final class FileReader extends Source<InputStream>  {


    @Override
    public InputStream provideFrom(String fileName) throws IOException {
        if (fileName.equals(Configuration.STDIN_PATH)) {
            return System.in;
        }
        File inputFile = new File(fileName);
        if (inputFile.isFile() && inputFile.canRead()) {
            return new FileInputStream(inputFile);
        }
        throw new IOException("Unable to read file [" + fileName + "]");
    }

}
