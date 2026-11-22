package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.lang.JPipeLexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

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
		lexer.addErrorListener(new ContextErrorListener(ctx));
		return new CommonTokenStream(lexer);
	}
}
