package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.actions.Action;
import ca.mcscert.jpipe.actions.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.actions.creation.CreateConclusion;
import ca.mcscert.jpipe.actions.creation.CreateEvidence;
import ca.mcscert.jpipe.actions.creation.CreateJustification;
import ca.mcscert.jpipe.actions.creation.CreatePattern;
import ca.mcscert.jpipe.actions.creation.CreateRelation;
import ca.mcscert.jpipe.actions.creation.CreateStrategy;
import ca.mcscert.jpipe.actions.creation.CreateSubConclusion;
import ca.mcscert.jpipe.actions.linking.CallOperator;
import ca.mcscert.jpipe.actions.linking.ImplementsPattern;
import ca.mcscert.jpipe.actions.linking.LoadFile;
import ca.mcscert.jpipe.actions.linking.Publish;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.operators.CompositionOperator;
import ca.mcscert.jpipe.syntax.JPipeBaseListener;
import ca.mcscert.jpipe.syntax.JPipeLexer;
import ca.mcscert.jpipe.syntax.JPipeParser;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Create a list of actions to build a model out of a parse tree.
 */
public final class ActionListProvider extends Transformation<ParseTree, List<Action>> {

    private static final Logger logger = LogManager.getLogger();

    /**
     * Transform a ParseTree into a list of actions to be executed to build the model.
     *
     * @param input the parse tree to visit.
     * @param source the name of the file that provided the parse tree (error mgmt)
     * @return the ordered list of Action instances to execute to create the model
     * @throws Exception is something goes wrong
     */
    @Override
    protected List<Action> run(ParseTree input, String source) throws Exception {
        ActionBuilder ab = new ActionBuilder(source);
        ParseTreeWalker.DEFAULT.walk(ab, input);
        logger.debug(ab.collect());
        return ab.collect();
    }

    /**
     * Creating the actions is a visit of the raw parse tree provided by ANTLR.
     */
    private static class ActionBuilder extends JPipeBaseListener {

        private record Context(String unitFileName, String justificationId) {
            public Context updateCurrentJustification(String id) {
                return new Context(this.unitFileName, id);
            }
        }

        private final List<Action> result;
        private Context buildContext;

        private CallOperator currentCall = null;

        public ActionBuilder(String name) {
            this.result = new ArrayList<>();
            this.buildContext = new Context(name, null);
        }

        public List<Action> collect() {
            return result;
        }

        /* ******************************** *
         * * Parsing Load file Directives * *
         * ******************************** */

        @Override
        public void enterLoad(JPipeParser.LoadContext ctx) {
            String relativePath = normalizePath(this.buildContext.unitFileName,
                                            strip(ctx.path.getText()));
            result.add(new LoadFile(relativePath));
        }

        /* ********************************************** *
         * * Parsing Justification/Patterns declaration * *
         * ********************************************** */

        @Override
        public void enterJustification(JPipeParser.JustificationContext ctx) {
            this.buildContext = buildContext.updateCurrentJustification(ctx.id.getText());
            if (ctx.parent == null) {
                result.add(new CreateJustification(ctx.id.getText()));
            } else {
                result.add(new CreateJustification(ctx.id.getText(), ctx.parent.getText()));
            }
        }

        @Override
        public void exitJustification(JPipeParser.JustificationContext ctx) {
            closeJustificationModel(ctx.parent, ctx.id);
            this.buildContext = buildContext.updateCurrentJustification(null);
        }

        @Override
        public void enterTemplate(JPipeParser.TemplateContext ctx) {
            this.buildContext = buildContext.updateCurrentJustification(ctx.id.getText());
            if (ctx.parent == null) {
                result.add(new CreatePattern(ctx.id.getText()));
            } else {
                result.add(new CreatePattern(ctx.id.getText(), ctx.parent.getText()));
            }
        }

        @Override
        public void exitTemplate(JPipeParser.TemplateContext ctx) {
            closeJustificationModel(ctx.parent, ctx.id);
            this.buildContext = buildContext.updateCurrentJustification(null);
        }

        /* ********************************** *
         * * Parsing justification elements * *
         * ********************************** */

        @Override
        public void enterEvidence(JPipeParser.EvidenceContext ctx) {
            String identifier = ctx.element().id.getText();
            result.add(new CreateEvidence(buildContext.justificationId,
                        identifier, strip(ctx.element().name.getText())));
        }



        @Override
        public void enterAbstract(JPipeParser.AbstractContext ctx) {
            String identifier = ctx.element().id.getText();
            result.add(new CreateAbstractSupport(buildContext.justificationId,
                    identifier, strip(ctx.element().name.getText())));
        }

        @Override
        public void enterStrategy(JPipeParser.StrategyContext ctx) {
            String identifier = ctx.element().id.getText();
            result.add(new CreateStrategy(buildContext.justificationId,
                        identifier, strip(ctx.element().name.getText())));
        }

        @Override
        public void enterConclusion(JPipeParser.ConclusionContext ctx) {
            String identifier = ctx.element().id.getText();
            result.add(new CreateConclusion(buildContext.justificationId,
                    identifier, strip(ctx.element().name.getText())));
        }

        @Override
        public void enterSub_conclusion(JPipeParser.Sub_conclusionContext ctx) {
            String identifier = ctx.element().id.getText();
            result.add(new CreateSubConclusion(buildContext.justificationId,
                    identifier, strip(ctx.element().name.getText())));
        }

        @Override
        public void enterRelation(JPipeParser.RelationContext ctx) {
            result.add(new CreateRelation(buildContext.justificationId,
                    ctx.from.getText(), ctx.to.getText()));
        }

        /* ***************************** *
         * * Parsing Composition units * *
         * ***************************** */


        @Override
        public void enterRule_decl(JPipeParser.Rule_declContext ctx) {
            CompositionOperator.ReturnType type = switch (ctx.type.getType()) {
                case JPipeLexer.JUSTIFICATION -> CompositionOperator.ReturnType.JUSTIFICATION;
                case JPipeLexer.TEMPLATE -> CompositionOperator.ReturnType.PATTERN;
                default -> throw new IllegalArgumentException(ctx.type.getText());
            };
            this.currentCall = new CallOperator(type, ctx.id.getText(), ctx.operator.getText());
        }

        @Override
        public void exitRule_decl(JPipeParser.Rule_declContext ctx) {
            result.add(this.currentCall);
            this.currentCall = null;
        }

        @Override
        public void enterParams_decl(JPipeParser.Params_declContext ctx) {
            this.currentCall.addInputModel(ctx.id.getText());

        }

        @Override
        public void enterKey_val_decl(JPipeParser.Key_val_declContext ctx) {
            this.currentCall.addParameter(ctx.key.getText(), strip(ctx.value.getText()));
        }

        /* ******************** *
         * * Helper functions * *
         * ******************** */

        private String strip(String s) {
            return s.substring(1, s.length() - 1);
        }

        private String normalizePath(String rootFile, String fileName) {
            Path root = Paths.get(rootFile).getParent();
            Path target = Paths.get(fileName);
            return root.resolve(target).toString();
        }

        private void closeJustificationModel(Token parent, Token id) {
            if (parent != null) {
                result.add(new ImplementsPattern(id.getText(), parent.getText()));
            } else {
                result.add(new Publish(id.getText()));
            }
        }

    }

}
