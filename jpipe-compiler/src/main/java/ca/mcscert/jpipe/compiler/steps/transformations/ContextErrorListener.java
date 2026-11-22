package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

/**
 * Reports the syntax errors ANTLR finds, while lexing or parsing, as non-fatal
 * diagnostics of the compilation.
 */
final class ContextErrorListener extends BaseErrorListener {

	private final CompilationContext ctx;

	ContextErrorListener(CompilationContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
			int line, int charPositionInLine, String msg,
			RecognitionException e) {
		ctx.error(line, charPositionInLine, msg);
	}
}
