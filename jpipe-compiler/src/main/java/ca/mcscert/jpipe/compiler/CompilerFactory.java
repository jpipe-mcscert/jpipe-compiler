package ca.mcscert.jpipe.compiler;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.compiler.model.ChainBuilder;
import ca.mcscert.jpipe.operators.OperatorRegistry;
import ca.mcscert.jpipe.operators.UnificationEquivalenceRegistry;
import ca.mcscert.jpipe.operators.builtin.AssembleOperator;
import ca.mcscert.jpipe.operators.builtin.RefineOperator;
import ca.mcscert.jpipe.operators.equivalences.SameLabel;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.compiler.steps.checkers.CompletenessChecker;
import ca.mcscert.jpipe.compiler.steps.checkers.ConsistencyChecker;
import ca.mcscert.jpipe.compiler.steps.io.sinks.ByteSink;
import ca.mcscert.jpipe.compiler.steps.io.sinks.StringSink;
import ca.mcscert.jpipe.compiler.steps.io.sources.FileSource;
import ca.mcscert.jpipe.compiler.steps.transformations.ActionListInterpretation;
import ca.mcscert.jpipe.compiler.steps.transformations.LoadResolver;
import ca.mcscert.jpipe.compiler.steps.transformations.CollectDiagnostics;
import ca.mcscert.jpipe.compiler.steps.transformations.DiagnosticRenderer;
import ca.mcscert.jpipe.compiler.steps.transformations.DiagnosticReport;
import ca.mcscert.jpipe.compiler.steps.transformations.DuplicateModelFilter;
import ca.mcscert.jpipe.compiler.steps.transformations.ExportToDot;
import ca.mcscert.jpipe.compiler.steps.transformations.ExportToJson;
import ca.mcscert.jpipe.compiler.steps.transformations.ExportToJpipe;
import ca.mcscert.jpipe.compiler.steps.transformations.ExportToPython;
import ca.mcscert.jpipe.compiler.steps.transformations.JsonDiagnosticReport;
import ca.mcscert.jpipe.compiler.steps.transformations.RenderWithDot;
import ca.mcscert.jpipe.compiler.steps.transformations.SelectModel;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Unit;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/**
 * Assembles a {@link Compiler} from a {@link CompilationConfig} by wiring the
 * appropriate pipeline steps.
 */
public final class CompilerFactory {

	private CompilerFactory() {
	}

	/**
	 * Build a process-mode compiler from the given configuration.
	 *
	 * @param config
	 *            the compilation configuration.
	 * @param stdout
	 *            stream to use when the output target is
	 *            {@link CompilationConfig#STDOUT}.
	 * @return a ready-to-use {@link Compiler}.
	 */
	public static Compiler build(CompilationConfig config,
			OutputStream stdout) {
		return switch (config.format()) {
			case JPIPE -> exporting(config, new ExportToJpipe())
					.andThen(new StringSink(stdout));
			case JSON -> exporting(config, new ExportToJson())
					.andThen(new StringSink(stdout));
			case PYTHON -> exporting(config, new ExportToPython())
					.andThen(new StringSink(stdout));
			case DOT -> exporting(config, new ExportToDot())
					.andThen(new StringSink(stdout));
			case PNG, JPEG, SVG -> exporting(config, new ExportToDot())
					.andThen(new RenderWithDot(
							config.format().name().toLowerCase(Locale.ROOT)))
					.andThen(new ByteSink(stdout));
		};
	}

	/**
	 * Build a diagnostic-mode compiler producing a human-readable report.
	 * Equivalent to
	 * {@link #buildDiagnosticCompiler(DiagnosticFormat, OutputStream)} with
	 * {@link DiagnosticFormat#TEXT}.
	 *
	 * @param stdout
	 *            stream to use when the output target is
	 *            {@link CompilationConfig#STDOUT}.
	 * @return a ready-to-use {@link Compiler}.
	 * @deprecated nothing in jPipe calls it; call
	 *             {@link #buildDiagnosticCompiler(DiagnosticFormat, OutputStream)}
	 *             with {@link DiagnosticFormat#TEXT}. To be removed in the next
	 *             major release.
	 */
	@Deprecated(since = "4.1.0", forRemoval = true)
	public static Compiler buildDiagnosticCompiler(OutputStream stdout) {
		return buildDiagnosticCompiler(DiagnosticFormat.TEXT, stdout);
	}

	/**
	 * Build a diagnostic-mode compiler that parses the source and reports on it
	 * without exporting any model.
	 *
	 * <p>
	 * Both formats share the {@link CollectDiagnostics} step and differ only in
	 * their renderer, so they always describe the same compilation.
	 *
	 * <p>
	 * The report is written by a {@link DiagnosticCompiler} rather than
	 * assembled as the tail of the analysis chain, so that a compilation which
	 * aborts on a fatal diagnostic is still reported on.
	 *
	 * @param format
	 *            how to render the report.
	 * @param stdout
	 *            stream to use when the output target is
	 *            {@link CompilationConfig#STDOUT}.
	 * @return a ready-to-use {@link Compiler}.
	 */
	public static Compiler buildDiagnosticCompiler(DiagnosticFormat format,
			OutputStream stdout) {
		DiagnosticRenderer renderer = switch (format) {
			case TEXT -> new DiagnosticReport();
			case JSON -> new JsonDiagnosticReport();
		};
		return new DiagnosticCompiler(new FileSource(),
				parsingChain().andThen(unitBuilder()).asTransformation(),
				renderer, new StringSink(stdout));
	}

	// -------------------------------------------------------------------------
	// Reusable sub-chains (also exposed for embedding in larger pipelines)
	// -------------------------------------------------------------------------

	/**
	 * Parsing chain: reads a source file, turns it into its action list with
	 * {@link LoadResolver#fileParser}, which aborts on any syntax error, then
	 * resolves its {@code load} directives.
	 */
	public static ChainBuilder<InputStream, List<Command>> parsingChain() {
		OperatorRegistry operators = builtInOperators();
		UnificationEquivalenceRegistry unification = builtInUnificationEquivalences();
		return new FileSource()
				.andThen(LoadResolver.fileParser(operators, unification))
				.andThen(new LoadResolver(operators, unification));
	}

	private static OperatorRegistry builtInOperators() {
		OperatorRegistry operators = new OperatorRegistry();
		operators.register("refine", new RefineOperator());
		operators.register("assemble", new AssembleOperator());
		return operators;
	}

	private static UnificationEquivalenceRegistry builtInUnificationEquivalences() {
		UnificationEquivalenceRegistry registry = new UnificationEquivalenceRegistry();
		registry.register("sameLabel", new SameLabel());
		return registry;
	}

	/**
	 * Full unit builder: extends the parsing chain with model construction,
	 * after dropping models declared twice, then consistency and completeness
	 * checking.
	 */
	public static Transformation<List<Command>, Unit> unitBuilder() {
		return new DuplicateModelFilter()
				.andThen(new ActionListInterpretation())
				.andThen(new ConsistencyChecker())
				.andThen(new CompletenessChecker());
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Parses the source, builds the unit, selects the requested model, and
	 * exports it with {@code exporter}.
	 */
	private static ChainBuilder<InputStream, String> exporting(
			CompilationConfig config,
			Transformation<JustificationModel, String> exporter) {
		return parsingChain().andThen(
				unitBuilder().andThen(new SelectModel(config.diagramName()))
						.andThen(exporter));
	}
}
