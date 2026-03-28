package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.lang.JPipeParser;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Delegate to ANTLR the transformation of a token stream into a parse tree.
 *
 * <p>
 * Parsing errors are reported as non-fatal diagnostics via the
 * {@link CompilationContext}, allowing the pipeline to accumulate both lexing
 * and parsing errors before deciding to abort.
 */
public final class Parser extends Transformation<CommonTokenStream, ParseTree> {

	@Override
	protected ParseTree run(CommonTokenStream tokens, CompilationContext ctx) {
		JPipeParser parser = new JPipeParser(tokens);
		parser.removeErrorListeners();
		parser.addErrorListener(new ParsingErrorListener(ctx));
		return parser.unit();
	}

	private static class ParsingErrorListener extends BaseErrorListener {

		private final CompilationContext ctx;

		ParsingErrorListener(CompilationContext ctx) {
			this.ctx = ctx;
		}

		@Override
		public void syntaxError(Recognizer<?, ?> recognizer,
				Object offendingSymbol, int line, int charPositionInLine,
				String msg, RecognitionException e) {
			ctx.error(line, charPositionInLine, msg);
		}
	}
}
