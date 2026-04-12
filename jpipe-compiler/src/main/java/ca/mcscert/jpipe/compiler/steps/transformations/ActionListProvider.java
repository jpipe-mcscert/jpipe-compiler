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
import ca.mcscert.jpipe.operators.ApplyOperator;
import ca.mcscert.jpipe.operators.ModelKind;
import ca.mcscert.jpipe.operators.OperatorRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Create a list of commands to build a model out of a parse tree.
 */
public final class ActionListProvider
		extends
			Transformation<ParseTree, List<Command>> {

	private final OperatorRegistry operators;

	public ActionListProvider(OperatorRegistry operators) {
		this.operators = operators;
	}

	@Override
	protected List<Command> run(ParseTree input, CompilationContext ctx)
			throws Exception {
		ActionBuilder ab = new ActionBuilder(ctx.sourcePath(), ctx, operators);
		ParseTreeWalker.DEFAULT.walk(ab, input);
		logger.debug(ab.collect());
		return ab.collect();
	}

	/**
	 * Creating the commands is a visit of the raw parse tree provided by ANTLR.
	 */
	private static class ActionBuilder extends JPipeBaseListener {

		private record Context(String unitFileName, String justificationId,
				String parentTemplateName) {
			public Context updateCurrentJustification(String id,
					String parentName) {
				return new Context(this.unitFileName, id, parentName);
			}
		}

		private final List<Command> result;
		private Context buildContext;
		private final CompilationContext compilationCtx;
		private final OperatorRegistry operators;
		private final Set<String> seenConclusionModels = new HashSet<>();

		public ActionBuilder(String name, CompilationContext compilationCtx,
				OperatorRegistry operators) {
			this.result = new ArrayList<>();
			this.buildContext = new Context(name, null, null);
			this.compilationCtx = compilationCtx;
			this.operators = operators;
		}

		public List<Command> collect() {
			return result;
		}

		@Override
		public void enterEveryRule(ParserRuleContext ctx) {
			String rule = JPipeParser.ruleNames[ctx.getRuleIndex()];
			int line = ctx.start.getLine();
			int col = ctx.start.getCharPositionInLine();
			String model = buildContext.justificationId();
			if (model != null) {
				logger.debug("enter: {} @ {}:{} [{}]", rule, line, col, model);
			} else {
				logger.debug("enter: {} @ {}:{}", rule, line, col);
			}
		}

		@Override
		public void exitEveryRule(ParserRuleContext ctx) {
			String rule = JPipeParser.ruleNames[ctx.getRuleIndex()];
			int line = ctx.start.getLine();
			int col = ctx.start.getCharPositionInLine();
			String model = buildContext.justificationId();
			if (model != null) {
				logger.debug("exit:  {} @ {}:{} [{}]", rule, line, col, model);
			} else {
				logger.debug("exit:  {} @ {}:{}", rule, line, col);
			}
		}

		/*
		 * ******************************** * * Parsing Load file Directives * *
		 * ********************************
		 */

		@Override
		public void enterLoad(JPipeParser.LoadContext ctx) {
			String raw = ctx.path.getText();
			// Strip surrounding quotes produced by the STRING lexer token
			String path = raw.substring(1, raw.length() - 1);
			String namespace = ctx.namespace != null
					? ctx.namespace.getText()
					: null;
			result.add(new LoadResolver.LoadDirective(path, namespace));
		}

		/*
		 * ********************************************** * * Parsing
		 * Justification/Patterns declaration * *
		 * **********************************************
		 */

		@Override
		public void enterJustification(JPipeParser.JustificationContext ctx) {
			SourceLocation loc = new SourceLocation(buildContext.unitFileName,
					ctx.id.getLine(), ctx.id.getCharPositionInLine());
			if (ctx.operator != null) {
				List<String> sources = ctx.params_decl() != null
						? ctx.params_decl().id.stream().map(t -> t.getText())
								.toList()
						: List.of();
				result.add(new ApplyOperator(ctx.id.getText(),
						ctx.operator.getText(), sources,
						collectConfig(ctx.rule_config()), operators, loc,
						ModelKind.JUSTIFICATION));
				return;
			}
			String parentName = ctx.parent != null
					? ctx.parent.getText()
					: null;
			this.buildContext = buildContext
					.updateCurrentJustification(ctx.id.getText(), parentName);
			result.add(new CreateJustification(ctx.id.getText(), loc));
			// ImplementsTemplate must be enqueued before body commands so that
			// inherited elements exist when override commands run.
			if (ctx.parent != null) {
				SourceLocation parentLoc = new SourceLocation(
						buildContext.unitFileName,
						ctx.parent.getStart().getLine(),
						ctx.parent.getStart().getCharPositionInLine());
				result.add(new ImplementsTemplate(ctx.id.getText(),
						ctx.parent.getText(), parentLoc));
			}
		}

		@Override
		public void exitJustification(JPipeParser.JustificationContext ctx) {
			this.buildContext = buildContext.updateCurrentJustification(null,
					null);
		}

		@Override
		public void enterTemplate(JPipeParser.TemplateContext ctx) {
			SourceLocation loc = new SourceLocation(buildContext.unitFileName,
					ctx.id.getLine(), ctx.id.getCharPositionInLine());
			if (ctx.operator != null) {
				List<String> sources = ctx.params_decl() != null
						? ctx.params_decl().id.stream().map(t -> t.getText())
								.toList()
						: List.of();
				result.add(new ApplyOperator(ctx.id.getText(),
						ctx.operator.getText(), sources,
						collectConfig(ctx.rule_config()), operators, loc,
						ModelKind.TEMPLATE));
				return;
			}
			String parentName = ctx.parent != null
					? ctx.parent.getText()
					: null;
			this.buildContext = buildContext
					.updateCurrentJustification(ctx.id.getText(), parentName);
			result.add(new CreateTemplate(ctx.id.getText(), loc));
			// ImplementsTemplate must be enqueued before body commands so that
			// inherited elements exist when override commands run.
			if (ctx.parent != null) {
				SourceLocation parentLoc = new SourceLocation(
						buildContext.unitFileName,
						ctx.parent.getStart().getLine(),
						ctx.parent.getStart().getCharPositionInLine());
				result.add(new ImplementsTemplate(ctx.id.getText(),
						ctx.parent.getText(), parentLoc));
			}
		}

		@Override
		public void exitTemplate(JPipeParser.TemplateContext ctx) {
			this.buildContext = buildContext.updateCurrentJustification(null,
					null);
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
				if (!validOverridePrefix(identifier, loc)) {
					return;
				}
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
				if (!validOverridePrefix(identifier, loc)) {
					return;
				}
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
			// full id (e.g. "t:s")
			String from = ctx.from.getText();
			String to = ctx.to.getText();
			SourceLocation loc = new SourceLocation(buildContext.unitFileName,
					ctx.from.getStart().getLine(),
					ctx.from.getStart().getCharPositionInLine());
			if (from.contains(":") && !validOverridePrefix(from, loc)) {
				return;
			}
			if (to.contains(":") && !validOverridePrefix(to, loc)) {
				return;
			}
			result.add(new AddSupport(buildContext.justificationId, to, from,
					loc));
		}

		/*
		 * ***************************** * * Parsing Composition units * *
		 * *****************************
		 */

		@Override
		public void enterRule_config(JPipeParser.Rule_configContext ctx) {
			// No-op: config key-value pairs are consumed eagerly in
			// enterJustification / enterTemplate.
		}

		private static Map<String, String> collectConfig(
				JPipeParser.Rule_configContext ctx) {
			if (ctx == null) {
				return Map.of();
			}
			Map<String, String> map = new LinkedHashMap<>();
			for (var kv : ctx.key_val_decl()) {
				map.put(kv.key.getText(), strip(kv.value.getText()));
			}
			return Collections.unmodifiableMap(map);
		}

		/*
		 * ******************** * * Helper functions * * ********************
		 */

		/**
		 * Returns true if the qualified {@code identifier} is allowed in the
		 * current model context.
		 */
		private boolean validOverridePrefix(String identifier,
				SourceLocation loc) {
			String parent = buildContext.parentTemplateName;
			if (parent == null) {
				compilationCtx.error(loc.line(), loc.column(),
						"[unresolved-override] '" + identifier
								+ "' is qualified but model '"
								+ buildContext.justificationId
								+ "' implements no template");
				return false;
			}
			// Permissive check: we only check if there is A parent.
			// The actual resolution is deferred to command execution via
			// findById.
			return true;
		}

		private static String strip(String s) {
			return s.substring(1, s.length() - 1);
		}
	}
}
