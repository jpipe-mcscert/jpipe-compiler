package ca.mcscert.jpipe.compiler;

import ca.mcscert.jpipe.compiler.parsing.PrintingListener;
import ca.mcscert.jpipe.lang.JPipeLexer;
import ca.mcscert.jpipe.lang.JPipeParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Walking skeleton: parses a .jpipe file and walks the parse tree.
 */
public class Compiler {

	public void compile(Path source) throws IOException {
		var lexer = new JPipeLexer(CharStreams.fromPath(source));
		var tokens = new CommonTokenStream(lexer);
		var parser = new JPipeParser(tokens);

		var tree = parser.unit();
		ParseTreeWalker.DEFAULT.walk(new PrintingListener(), tree);
	}
}
