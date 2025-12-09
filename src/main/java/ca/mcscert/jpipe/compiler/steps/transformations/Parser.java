package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.error.ParsingError;
import ca.mcscert.jpipe.syntax.JPipeParser;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Delegate to ANTLR the transformation of a token stream into a parse tree.
 */
public final class Parser extends Transformation<CommonTokenStream, ParseTree> {

    private final List<Throwable> errors;

    /**
     * The parser step might produce errors. As such, the constructor takes as input a list, used to
     * record lexing errors when encountered. We delegate the responsibility of "what to do with
     * such errors" to next steps.
     *
     * @param errors the list of errors used to record parsing errors, if any.
     */
    public Parser(List<Throwable> errors) {
        this.errors = errors;
    }

    @Override
    protected ParseTree run(CommonTokenStream tokens, String source) throws Exception {
        JPipeParser parser = new JPipeParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(new ParsingErrorListener(errors, source));
        return parser.unit();
    }

    /**
     * Specialization of the ANTLR error listener to implement error recording instead of
     * exception throwing.
     */
    private static class ParsingErrorListener extends BaseErrorListener {

        private final List<Throwable> errors;
        private final String fileName;


        public ParsingErrorListener(List<Throwable> errors, String fileName) {
            this.fileName = fileName;
            this.errors = errors;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg,
                                RecognitionException e) {
            errors.add(new ParsingError(fileName, line, charPositionInLine, msg));
        }


    }


}
