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
import ca.mcscert.jpipe.compiler.model.DiagnosticCodes;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.lang.JPipeBaseListener;
import ca.mcscert.jpipe.lang.JPipeParser;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.operators.ApplyOperator;
import ca.mcscert.jpipe.operators.ModelKind;
import ca.mcscert.jpipe.operators.OperatorCallConfig;
import ca.mcscert.jpipe.operators.OperatorRegistry;
import ca.mcscert.jpipe.operators.UnificationEquivalenceRegistry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

/**
 * Create a list of commands to build a model out of a parse tree.
 */
public final class ActionListProvider
		extends
			Transformation<ParseTree, List<Command>> {

	private final OperatorRegistry operators;
	private final UnificationEquivalenceRegistry unificationEquivalences;

	public ActionListProvider(OperatorRegistry operators,
			UnificationEquivalenceRegistry unificationEquivalences) {
		this.operators = operators;
		this.unificationEquivalences = unificationEquivalences;
	}

	@Override
	protected List<Command> run(ParseTree input, CompilationContext ctx) {
		ActionBuilder ab = new ActionBuilder(ctx.sourcePath(), ctx, operators,
				unificationEquivalences);
		ParseTreeWalker.DEFAULT.walk(ab, input);
		logger.debug(ab.collect());
		return ab.collect();
	}

	/**
	 * Creating the commands is a visit of the raw parse tree provided by ANTLR.
	 */
	private static class ActionBuilder extends JPipeBaseListener {

		/** Creates a support leaf: {@code CreateEvidence::new}, … */
		@FunctionalInterface
		private interface LeafCreation {
			Command create(String model, String id, String label,
					SourceLocation location);
		}

		private final List<Command> result;
		/** The file being read, named in every location. */
		private final String unitFileName;
		/** The model whose declaration is being read, if any. */
		private String currentModel;
		/** The template the current model implements, if any. */
		private String parentTemplateName;
		private final CompilationContext compilationCtx;
		private final OperatorRegistry operators;
		private final UnificationEquivalenceRegistry unificationEquivalences;
		/**
		 * Whether the model declaration being read already declared a
		 * conclusion. Tracked per declaration, not per name: two models that
		 * share a name are a duplicate-model error, not a model with two
		 * conclusions (#183).
		 */
		private boolean conclusionDeclared;

		/**
		 * The conclusions the declaration being read declared although it
		 * inherits one, and which were therefore dropped. The relations of the
		 * declaration that use them are dropped too, once its body has been
		 * read, wherever they are written: reporting them as unknown elements
		 * would only repeat the inherited-conclusion error (#177).
		 */
		private final Set<String> rejectedConclusions = new HashSet<>();

		/** Where the commands of the declaration being read start. */
		private int declarationStart;

		public ActionBuilder(String name, CompilationContext compilationCtx,
				OperatorRegistry operators,
				UnificationEquivalenceRegistry unificationEquivalences) {
			this.result = new ArrayList<>();
			this.unitFileName = name;
			this.compilationCtx = compilationCtx;
			this.operators = operators;
			this.unificationEquivalences = unificationEquivalences;
		}

		public List<Command> collect() {
			return result;
		}

		@Override
		public void enterEveryRule(ParserRuleContext ctx) {
			logRule("enter:", ctx);
		}

		@Override
		public void exitEveryRule(ParserRuleContext ctx) {
			logRule("exit: ", ctx);
		}

		/**
		 * Traces one parse-tree node. Called on every enter and exit, so it
		 * returns before computing anything when DEBUG is off.
		 */
		private void logRule(String event, ParserRuleContext ctx) {
			if (!logger.isDebugEnabled()) {
				return;
			}
			String rule = JPipeParser.ruleNames[ctx.getRuleIndex()];
			int line = ctx.start.getLine();
			int col = ctx.start.getCharPositionInLine();
			String model = currentModel;
			if (model != null) {
				logger.debug("{} {} @ {}:{} [{}]", event, rule, line, col,
						model);
			} else {
				logger.debug("{} {} @ {}:{}", event, rule, line, col);
			}
		}

		/*
		 * ******************************** * * Parsing Load file Directives * *
		 * ********************************
		 */

		@Override
		public void enterLoad(JPipeParser.LoadContext ctx) {
			String raw = ctx.path.getText();
			String path = StringLiteral.value(raw);
			String namespace = ctx.namespace != null
					? ctx.namespace.getText()
					: null;
			result.add(new LoadResolver.LoadDirective(path, namespace,
					locationOf(ctx.getStart())));
		}

		/*
		 * ********************************************** * * Parsing
		 * Justification/Patterns declaration * *
		 * **********************************************
		 */

		@Override
		public void enterJustification(JPipeParser.JustificationContext ctx) {
			enterModel(ctx.id, ctx.parent, ctx.operator, ctx.params_decl(),
					ctx.rule_config(), ModelKind.JUSTIFICATION);
		}

		/**
		 * Drops the relations of the declaration just read that use a rejected
		 * conclusion: see rejectedConclusions.
		 */
		private void dropRelationsToRejectedConclusions() {
			if (rejectedConclusions.isEmpty()) {
				return;
			}
			result.subList(declarationStart, result.size()).removeIf(
					cmd -> cmd instanceof AddSupport a && (rejectedConclusions
							.contains(a.supportableId())
							|| rejectedConclusions.contains(a.supporterId())));
		}

		@Override
		public void exitJustification(JPipeParser.JustificationContext ctx) {
			exitModel();
		}

		@Override
		public void enterTemplate(JPipeParser.TemplateContext ctx) {
			enterModel(ctx.id, ctx.parent, ctx.operator, ctx.params_decl(),
					ctx.rule_config(), ModelKind.TEMPLATE);
		}

		/**
		 * Shared by justification and template declarations, which the grammar
		 * shapes alike: a model is either the result of an operator call, or
		 * declared with a body, optionally implementing a template.
		 */
		private void enterModel(Token id,
				JPipeParser.Qualified_idContext parent,
				JPipeParser.Qualified_idContext operator,
				JPipeParser.Params_declContext params,
				JPipeParser.Rule_configContext config, ModelKind kind) {
			String name = id.getText();
			SourceLocation loc = locationOf(id);
			if (operator != null) {
				if (parent != null) {
					// The grammar allows it, but nothing would apply it: the
					// result's structure is the operator's (#184).
					compilationCtx.error(DiagnosticCodes.INVALID_OPERATOR_CALL,
							locationOf(parent.getStart()),
							"'" + name + "' is built by '" + operator.getText()
									+ "': it cannot also implement '"
									+ parent.getText() + "'");
				}
				List<String> sources = params != null
						? params.id.stream().map(ParserRuleContext::getText)
								.toList()
						: List.of();
				var callConfig = new OperatorCallConfig(name,
						operator.getText(), sources, collectConfig(config), loc,
						kind);
				result.add(new ApplyOperator(callConfig, operators,
						unificationEquivalences));
				return;
			}
			String parentName = parent != null ? parent.getText() : null;
			this.currentModel = name;
			this.parentTemplateName = parentName;
			this.conclusionDeclared = false;
			this.rejectedConclusions.clear();
			this.declarationStart = result.size();
			result.add(switch (kind) {
				case JUSTIFICATION -> new CreateJustification(name, loc);
				case TEMPLATE -> new CreateTemplate(name, loc);
			});
			// ImplementsTemplate must be enqueued before body commands so that
			// inherited elements exist when override commands run.
			if (parent != null) {
				result.add(new ImplementsTemplate(name, parentName,
						locationOf(parent.getStart())));
			}
		}

		@Override
		public void exitTemplate(JPipeParser.TemplateContext ctx) {
			exitModel();
		}

		/** Shared by justification and template declarations. */
		private void exitModel() {
			dropRelationsToRejectedConclusions();
			this.currentModel = null;
			this.parentTemplateName = null;
		}

		/*
		 * ********************************** * * Parsing justification elements
		 * * * **********************************
		 */

		@Override
		public void enterEvidence(JPipeParser.EvidenceContext ctx) {
			enterSupportLeaf(ctx.element(), "evidence", CreateEvidence::new);
		}

		@Override
		public void enterSub_conclusion(JPipeParser.Sub_conclusionContext ctx) {
			enterSupportLeaf(ctx.element(), "sub-conclusion",
					CreateSubConclusion::new);
		}

		/**
		 * Shared by evidence and sub-conclusions: a qualified id overrides an
		 * inherited {@code @support}, a plain one declares a new leaf.
		 */
		private void enterSupportLeaf(JPipeParser.ElementContext element,
				String kind, LeafCreation creation) {
			String identifier = element.id.getText();
			SourceLocation loc = locationOf(element);
			if (!identifier.contains(":")) {
				result.add(creation.create(currentModel, identifier,
						labelOf(element), loc));
			} else if (validOverridePrefix(identifier, loc)) {
				result.add(new OverrideAbstractSupport(currentModel, identifier,
						kind, labelOf(element), loc));
			}
		}

		@Override
		public void enterAbstract(JPipeParser.AbstractContext ctx) {
			result.add(new CreateAbstractSupport(currentModel,
					ctx.element().id.getText(), labelOf(ctx.element()),
					locationOf(ctx.element())));
		}

		@Override
		public void enterStrategy(JPipeParser.StrategyContext ctx) {
			result.add(
					new CreateStrategy(currentModel, ctx.element().id.getText(),
							labelOf(ctx.element()), locationOf(ctx.element())));
		}

		@Override
		public void enterConclusion(JPipeParser.ConclusionContext ctx) {
			String identifier = ctx.element().id.getText();
			SourceLocation loc = locationOf(ctx.element());
			if (parentTemplateName != null) {
				// A model that implements a template inherits its conclusion
				// (ADR-0012). Decided here, from the declaration alone, it no
				// longer depends on which of the two is declared first (#177).
				rejectedConclusions.add(identifier);
				compilationCtx.error(DiagnosticCodes.INHERITED_CONCLUSION, loc,
						"Model '" + currentModel + "' implements '"
								+ parentTemplateName
								+ "' and inherits its conclusion: it cannot"
								+ " declare its own");
				return;
			}
			if (conclusionDeclared) {
				compilationCtx.error(DiagnosticCodes.SINGLE_CONCLUSION, loc,
						"Model '" + currentModel
								+ "' declares multiple conclusions");
				return;
			}
			conclusionDeclared = true;
			result.add(new CreateConclusion(currentModel, identifier,
					labelOf(ctx.element()), loc));
		}

		@Override
		public void enterRelation(JPipeParser.RelationContext ctx) {
			// ctx.from/ctx.to are qualified_id contexts; getText() returns the
			// full id (e.g. "t:s")
			String from = ctx.from.getText();
			String to = ctx.to.getText();
			SourceLocation loc = locationOf(ctx.from.getStart());
			if (from.contains(":") && !validOverridePrefix(from, loc)) {
				return;
			}
			if (to.contains(":") && !validOverridePrefix(to, loc)) {
				return;
			}
			result.add(new AddSupport(currentModel, to, from, loc));
		}

		/*
		 * ***************************** * * Parsing Composition units * *
		 * *****************************
		 */

		/**
		 * The arguments of an operator call. A key given twice is reported, and
		 * its first value kept: the last one used to win silently (#184).
		 */
		private Map<String, String> collectConfig(
				JPipeParser.Rule_configContext ctx) {
			if (ctx == null) {
				return Map.of();
			}
			Map<String, String> map = new LinkedHashMap<>();
			for (var kv : ctx.key_val_decl()) {
				String key = kv.key.getText();
				if (map.containsKey(key)) {
					compilationCtx.error(DiagnosticCodes.INVALID_OPERATOR_CALL,
							locationOf(kv.key),
							"argument '" + key + "' is given more than once");
					continue;
				}
				map.put(key, StringLiteral.value(kv.value.getText()));
			}
			return Collections.unmodifiableMap(map);
		}

		/*
		 * ******************** * * Helper functions * * ********************
		 */

		/** Where {@code token} sits in the file being compiled. */
		private SourceLocation locationOf(Token token) {
			return new SourceLocation(unitFileName, token.getLine(),
					token.getCharPositionInLine());
		}

		/** Where {@code element} is declared: at its id. */
		private SourceLocation locationOf(JPipeParser.ElementContext element) {
			return locationOf(element.id.getStart());
		}

		/** The label of {@code element}, unquoted. */
		private static String labelOf(JPipeParser.ElementContext element) {
			return StringLiteral.value(element.name.getText());
		}

		/**
		 * Returns true if the qualified {@code identifier} is allowed in the
		 * current model context.
		 */
		private boolean validOverridePrefix(String identifier,
				SourceLocation loc) {
			if (parentTemplateName == null) {
				compilationCtx.error(DiagnosticCodes.UNRESOLVED_OVERRIDE, loc,
						"'" + identifier + "' is qualified but model '"
								+ currentModel + "' implements no template");
				return false;
			}
			// Permissive check: we only check if there is A parent.
			// The actual resolution is deferred to command execution via
			// findById.
			return true;
		}
	}
}
