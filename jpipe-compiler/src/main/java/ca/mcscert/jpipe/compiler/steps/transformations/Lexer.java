package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.lang.JPipeLexer;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Delegate to ANTLR the task of "lexing" the character stream into relevant
 * tokens.
 *
 * <p>
 * Lexing errors are reported as non-fatal diagnostics via the
 * {@link CompilationContext}, allowing subsequent steps (e.g. the parser) to
 * still run and accumulate further errors before the pipeline decides to abort.
 */
public final class Lexer extends Transformation<CharStream, CommonTokenStream> {

	@Override
	protected CommonTokenStream run(CharStream input, CompilationContext ctx) {
		JPipeLexer lexer = new JPipeLexer(input);
		lexer.removeErrorListeners();
		lexer.addErrorListener(new LexerErrorListener(ctx));
		return new CommonTokenStream(lexer);
	}

	private static class LexerErrorListener extends BaseErrorListener {

		private final CompilationContext ctx;

		LexerErrorListener(CompilationContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e) {
			ctx.error(line, charPositionInLine, msg);
		}
	}
}
