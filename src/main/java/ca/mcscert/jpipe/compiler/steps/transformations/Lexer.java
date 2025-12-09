package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.error.LexingError;
import ca.mcscert.jpipe.syntax.JPipeLexer;
import java.util.List;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Delegate to ANTLR the task of "lexing" the character stream into relevant tokens.
 */
public final class Lexer extends Transformation<CharStream, CommonTokenStream> {

    private final List<Throwable> errors;

    /**
     * The lexer step might produce errors. As such, the constructor takes as input a list, used to
     * record lexing errors when encountered. We delegate the responsibility of "what to do with
     * such errors" to next steps.
     *
     * @param errors the list of errors used to record lexing errors, if any.
     */
    public Lexer(List<Throwable> errors) {
        this.errors = errors;
    }

    @Override
    protected CommonTokenStream run(CharStream input, String source) throws Exception {
        JPipeLexer lexer = new JPipeLexer(input);
        lexer.removeErrorListeners();
        lexer.addErrorListener(new LexerErrorListener(errors, source));
        return new CommonTokenStream(lexer);
    }

    /**
     * Specialization of the ANTLR error listener to implement error recording instead of
     * exception throwing.
     */
    private static class LexerErrorListener extends BaseErrorListener {

        private final List<Throwable> errors;
        private final String fileName;

        public LexerErrorListener(List<Throwable> errors, String fileName) {
            this.errors = errors;
            this.fileName = fileName;
        }

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                int line, int charPositionInLine, String msg,
                                RecognitionException e) {
            errors.add(new LexingError(fileName, line, charPositionInLine, msg));
        }

    }

}
