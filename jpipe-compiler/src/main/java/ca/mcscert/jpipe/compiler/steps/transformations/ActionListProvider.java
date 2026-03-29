package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.commands.linking.OverrideAbstractSupport;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.lang.JPipeBaseListener;
import ca.mcscert.jpipe.lang.JPipeParser;
import ca.mcscert.jpipe.model.SourceLocation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Create a list of commands to build a model out of a parse tree.
 */
public final class ActionListProvider
		extends
			Transformation<ParseTree, List<Command>> {

	@Override
	protected List<Command> run(ParseTree input, CompilationContext ctx)
			throws Exception {
		ActionBuilder ab = new ActionBuilder(ctx.sourcePath(), ctx);
		ParseTreeWalker.DEFAULT.walk(ab, input);
		logger.debug(ab.collect());
		return ab.collect();
	}

	/**
	 * Creating the commands is a visit of the raw parse tree provided by ANTLR.
	 */
	private static class ActionBuilder extends JPipeBaseListener {

		private record Context(String unitFileName, String justificationId) {
			public Context updateCurrentJustification(String id) {
				return new Context(this.unitFileName, id);
			}
		}

		private final List<Command> result;
		private Context buildContext;
		private final CompilationContext compilationCtx;
		private final Set<String> seenConclusionModels = new HashSet<>();

		public ActionBuilder(String name, CompilationContext compilationCtx) {
			this.result = new ArrayList<>();
			this.buildContext = new Context(name, null);
			this.compilationCtx = compilationCtx;
		}

		public List<Command> collect() {
			return result;
		}

		/*
		 * ******************************** * * Parsing Load file Directives * *
		 * ********************************
		 */

		@Override
		public void enterLoad(JPipeParser.LoadContext ctx) {
			throw new UnsupportedOperationException(
					"load directive is not yet supported in the refactored pipeline");
		}

		/*
		 * ********************************************** * * Parsing
		 * Justification/Patterns declaration * *
		 * **********************************************
		 */

		@Override
		public void enterJustification(JPipeParser.JustificationContext ctx) {
			this.buildContext = buildContext
					.updateCurrentJustification(ctx.id.getText());
			SourceLocation loc = new SourceLocation(buildContext.unitFileName,
					ctx.id.getLine(), ctx.id.getCharPositionInLine());
			result.add(new CreateJustification(ctx.id.getText(), loc));
		}

		@Override
		public void exitJustification(JPipeParser.JustificationContext ctx) {
			closeJustificationModel(ctx.parent, ctx.id);
			this.buildContext = buildContext.updateCurrentJustification(null);
		}

		@Override
		public void enterTemplate(JPipeParser.TemplateContext ctx) {
			this.buildContext = buildContext
					.updateCurrentJustification(ctx.id.getText());
			SourceLocation loc = new SourceLocation(buildContext.unitFileName,
					ctx.id.getLine(), ctx.id.getCharPositionInLine());
			result.add(new CreateTemplate(ctx.id.getText(), loc));
		}

		@Override
		public void exitTemplate(JPipeParser.TemplateContext ctx) {
			closeJustificationModel(ctx.parent, ctx.id);
			this.buildContext = buildContext.updateCurrentJustification(null);
		}

		/*
		 * ********************************** * * Parsing justification elements
		 * * * **********************************
		 */

		@Override
		public void enterEvidence(JPipeParser.EvidenceContext ctx) {
			String identifier = ctx.element().id.getText();
			String label = strip(ctx.element().name.getText());
			SourceLocation loc = new SourceLocation(buildContext.unitFileName,
					ctx.element().id.getStart().getLine(),
					ctx.element().id.getStart().getCharPositionInLine());
			if (identifier.contains(":")) {
				result.add(new OverrideAbstractSupport(
						buildContext.justificationId, identifier, "evidence",
						label, loc));
			} else {
				result.add(new CreateEvidence(buildContext.justificationId,
						identifier, label, loc));
			}
		}

		@Override
		public void enterAbstract(JPipeParser.AbstractContext ctx) {
			String identifier = ctx.element().id.getText();
			SourceLocation loc = new SourceLocation(buildContext.unitFileName,
					ctx.element().id.getStart().getLine(),
					ctx.element().id.getStart().getCharPositionInLine());
			result.add(new CreateAbstractSupport(buildContext.justificationId,
					identifier, strip(ctx.element().name.getText()), loc));
		}

		@Override
		public void enterStrategy(JPipeParser.StrategyContext ctx) {
			String identifier = ctx.element().id.getText();
			SourceLocation loc = new SourceLocation(buildContext.unitFileName,
					ctx.element().id.getStart().getLine(),
					ctx.element().id.getStart().getCharPositionInLine());
			result.add(new CreateStrategy(buildContext.justificationId,
					identifier, strip(ctx.element().name.getText()), loc));
		}

		@Override
		public void enterConclusion(JPipeParser.ConclusionContext ctx) {
			String identifier = ctx.element().id.getText();
			SourceLocation loc = new SourceLocation(buildContext.unitFileName,
					ctx.element().id.getStart().getLine(),
					ctx.element().id.getStart().getCharPositionInLine());
			String modelId = buildContext.justificationId;
			if (!seenConclusionModels.add(modelId)) {
				compilationCtx.error(loc.line(), loc.column(),
						"[single-conclusion] Model '" + modelId
								+ "' declares multiple conclusions");
				return;
			}
			result.add(new CreateConclusion(modelId, identifier,
					strip(ctx.element().name.getText()), loc));
		}

		@Override
		public void enterSub_conclusion(JPipeParser.Sub_conclusionContext ctx) {
			String identifier = ctx.element().id.getText();
			String label = strip(ctx.element().name.getText());
			SourceLocation loc = new SourceLocation(buildContext.unitFileName,
					ctx.element().id.getStart().getLine(),
					ctx.element().id.getStart().getCharPositionInLine());
			if (identifier.contains(":")) {
				result.add(new OverrideAbstractSupport(
						buildContext.justificationId, identifier,
						"sub-conclusion", label, loc));
			} else {
				result.add(new CreateSubConclusion(buildContext.justificationId,
						identifier, label, loc));
			}
		}

		@Override
		public void enterRelation(JPipeParser.RelationContext ctx) {
			// ctx.from/ctx.to are qualified_id contexts; getText() returns the
			// full id
			// (e.g. "t:s")
			result.add(new AddSupport(buildContext.justificationId,
					ctx.to.getText(), ctx.from.getText()));
		}

		/*
		 * ***************************** * * Parsing Composition units * *
		 * *****************************
		 */

		@Override
		public void enterRule_config(JPipeParser.Rule_configContext ctx) {
			throw new UnsupportedOperationException(
					"composition operators are not yet supported in the refactored pipeline");
		}

		/*
		 * ******************** * * Helper functions * * ********************
		 */

		private String strip(String s) {
			return s.substring(1, s.length() - 1);
		}

		private void closeJustificationModel(Token parent, Token id) {
			if (parent != null) {
				result.add(
						new ImplementsTemplate(id.getText(), parent.getText()));
			}
			// standalone models (no implements clause) have null parent by
			// default;
			// no command needed here — locking belongs to a downstream checker
			// step
		}
	}
}
