package ca.mcscert.jpipe.compiler.parsing;

import ca.mcscert.jpipe.lang.JPipeParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * Walking skeleton: prints the name of each grammar rule as the parse tree is
 * traversed.
 */
public class PrintingListener implements ParseTreeListener {

	private int depth = 0;

	@Override
	public void enterEveryRule(ParserRuleContext ctx) {
		String ruleName = JPipeParser.ruleNames[ctx.getRuleIndex()];
		System.out.println("  ".repeat(depth) + "enter: " + ruleName);
		depth++;
	}

	@Override
	public void exitEveryRule(ParserRuleContext ctx) {
		depth--;
	}

	@Override
	public void visitTerminal(TerminalNode node) {
		System.out.println("  ".repeat(depth) + "token: " + node.getText());
	}

	@Override
	public void visitErrorNode(org.antlr.v4.runtime.tree.ErrorNode node) {
		System.out.println("  ".repeat(depth) + "error: " + node.getText());
	}
}
