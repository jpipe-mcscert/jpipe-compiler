package ca.mcscert.jpipe.compiler;

import ca.mcscert.jpipe.actions.Action;
import ca.mcscert.jpipe.cli.Configuration;
import ca.mcscert.jpipe.compiler.model.ChainBuilder;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.compiler.steps.checkers.CompletenessChecker;
import ca.mcscert.jpipe.compiler.steps.checkers.ConsistencyChecker;
import ca.mcscert.jpipe.compiler.steps.checkers.LazyHaltAndCatchFire;
import ca.mcscert.jpipe.compiler.steps.io.sinks.GraphVizRenderer;
import ca.mcscert.jpipe.compiler.steps.io.sinks.OutputStreamWriter;
import ca.mcscert.jpipe.compiler.steps.io.sources.FileReader;
import ca.mcscert.jpipe.compiler.steps.transformations.ActionListInterpretation;
import ca.mcscert.jpipe.compiler.steps.transformations.ActionListProvider;
import ca.mcscert.jpipe.compiler.steps.transformations.CharStreamProvider;
import ca.mcscert.jpipe.compiler.steps.transformations.LambdaExecution;
import ca.mcscert.jpipe.compiler.steps.transformations.Lexer;
import ca.mcscert.jpipe.compiler.steps.transformations.ModelVisit;
import ca.mcscert.jpipe.compiler.steps.transformations.Parser;
import ca.mcscert.jpipe.compiler.steps.transformations.ScopeFiltering;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.visitors.exporters.GraphVizExporter;
import ca.mcscert.jpipe.visitors.exporters.JpipeExporter;
import ca.mcscert.jpipe.visitors.exporters.JpipeRunnerExporter;
import ca.mcscert.jpipe.visitors.exporters.JsonExporter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory pattern to assemble steps, sink and sources into consistent compilers, based on the
 * provided configuration info from the command-line.
 */
public final class CompilerFactory {

    /**
     * Build a tailored compiler out of a provided configuration.
     *
     * @param config the configuration obtained from the command line.
     * @return the expected compiler.
     */
    public static Compiler build(Configuration config) {
        return switch (config.getMode()) {
            case PRINT -> processPrintMode(unitBuilder(), config);
            case LIST  -> processListMode(unitBuilder(), config);
        };
    }

    /**
     * Provide a partial compilation chain to get actions out of a given filename.
     *
     * @return a default transformation from an input stream to a list of actions.
     */
    public static Transformation<InputStream, List<Action>> actionProvider() {
        return minimalChain().asTransformation();
    }

    /**
     * Provide a transformation that build a unit from a given input stream.
     *
     * @return a compiled unit.
     */
    public static Transformation<InputStream, Unit> loader() {
        return unitBuilder().asTransformation();
    }

    private static Compiler processPrintMode(ChainBuilder<InputStream, Unit> unitBuilder,
                                             Configuration config) {
        ChainBuilder<InputStream, JustificationModel> chain =
                unitBuilder.andThen(new ScopeFiltering(config.getDiagramName()));

        return switch (config.getFormat()) {
            case PNG, DOT, SVG -> chain.andThen(new ModelVisit<>(new GraphVizExporter()))
                                        .andThen(new GraphVizRenderer(config.getFormat()));
            case JPIPE -> chain.andThen(new ModelVisit<>(new JpipeExporter()))
                               .andThen(new OutputStreamWriter<>());
            case JSON -> chain.andThen(new ModelVisit<>(new JsonExporter()))
                              .andThen(new LambdaExecution<>((json) -> json.toString(2)))
                              .andThen(new OutputStreamWriter<>());
            case RUNNER -> chain.andThen(new ModelVisit<>(new JpipeRunnerExporter()))
                                .andThen(new OutputStreamWriter<>());
        };

    }

    private static Compiler processListMode(ChainBuilder<InputStream, Unit> unitBuilder,
                                            Configuration config) {
        throw new UnsupportedOperationException("LIST mode not supported in this version");
    }

    /**
     * Represent the minimal compilation stapes to extract actions from a source file.
     *
     * @return the list of actions to execute to build the model (a unit).
     */
    private static ChainBuilder<InputStream, List<Action>> minimalChain() {
        List<Throwable> errors = new ArrayList<>();
        return new FileReader()
                .andThen(new CharStreamProvider())
                .andThen(new Lexer(errors))
                .andThen(new Parser(errors))
                .andThen(new LazyHaltAndCatchFire<>(errors))
                .andThen(new ActionListProvider());
    }

    /**
     * Extend minimal chain by interpreting the actions and checking consistency/completeness.
     *
     * @return a complete and consistent unit, if any.
     */
    private static ChainBuilder<InputStream, Unit> unitBuilder() {
        return minimalChain()
                .andThen(new ActionListInterpretation())
                .andThen(new CompletenessChecker())
                .andThen(new ConsistencyChecker());
    }


}
