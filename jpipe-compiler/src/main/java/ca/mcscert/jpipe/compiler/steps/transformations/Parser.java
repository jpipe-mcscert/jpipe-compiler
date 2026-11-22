package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.lang.JPipeParser;
import org.antlr.v4.runtime.CommonTokenStream;
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
		parser.addErrorListener(new ContextErrorListener(ctx));
		return parser.unit();
	}
}
