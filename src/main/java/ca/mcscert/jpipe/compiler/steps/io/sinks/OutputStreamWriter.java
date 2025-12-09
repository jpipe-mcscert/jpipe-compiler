package ca.mcscert.jpipe.compiler.steps.io.sinks;

import ca.mcscert.jpipe.cli.Configuration;
import ca.mcscert.jpipe.compiler.model.Sink;
import ca.mcscert.jpipe.error.ErrorManager;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Flush to an output stream any object that support a .toString() call.
 *
 * @param <T> the type of object we're publishing.
 */
public final class OutputStreamWriter<T> implements Sink<T> {

    @Override
    public final void pourInto(T output, String fileName) throws IOException {
        if (fileName.equals(Configuration.STDOUT_PATH)) {
            System.out.println(output.toString());
            System.out.flush();
        } else {
            try (PrintStream fos = new PrintStream(fileName)) {
                fos.println(output.toString());
                fos.flush();
            } catch (IOException ioe) {
                ErrorManager.getInstance().fatal(ioe);
            }
        }
    }
}
