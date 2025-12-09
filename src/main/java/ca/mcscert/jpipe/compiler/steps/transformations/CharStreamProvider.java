package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.Transformation;
import java.io.IOException;
import java.io.InputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;

/**
 * Delegate to ANTLR the transformation of an input stream into a character-based one.
 */
public final class CharStreamProvider extends Transformation<InputStream, CharStream> {

    @Override
    protected CharStream run(InputStream input, String source) throws IOException {
        return CharStreams.fromStream(input);
    }
}
