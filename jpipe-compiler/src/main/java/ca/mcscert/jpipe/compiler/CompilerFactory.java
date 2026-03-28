package ca.mcscert.jpipe.compiler;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.compiler.model.ChainBuilder;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.compiler.steps.checkers.HaltAndCatchFire;
import ca.mcscert.jpipe.compiler.steps.io.sinks.ByteSink;
import ca.mcscert.jpipe.compiler.steps.io.sinks.StringSink;
import ca.mcscert.jpipe.compiler.steps.io.sources.FileSource;
import ca.mcscert.jpipe.compiler.steps.transformations.ActionListInterpretation;
import ca.mcscert.jpipe.compiler.steps.transformations.ActionListProvider;
import ca.mcscert.jpipe.compiler.steps.transformations.CharStreamProvider;
import ca.mcscert.jpipe.compiler.steps.transformations.DiagnosticReport;
import ca.mcscert.jpipe.compiler.steps.transformations.ExportToDot;
import ca.mcscert.jpipe.compiler.steps.transformations.ExportToJson;
import ca.mcscert.jpipe.compiler.steps.transformations.ExportToJpipe;
import ca.mcscert.jpipe.compiler.steps.transformations.ExportToPython;
import ca.mcscert.jpipe.compiler.steps.transformations.Lexer;
import ca.mcscert.jpipe.compiler.steps.transformations.Parser;
import ca.mcscert.jpipe.compiler.steps.transformations.RenderWithDot;
import ca.mcscert.jpipe.compiler.steps.transformations.SelectModel;
import ca.mcscert.jpipe.model.Unit;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.antlr.v4.runtime.tree.ParseTree;

/**
 * Assembles a {@link Compiler} from a {@link CompilationConfig} by wiring the
 * appropriate pipeline steps.
 *
 * <p>
 * Not all modes and formats are implemented yet. Unsupported combinations throw
 * {@link UnsupportedOperationException} at factory time, before any file I/O
 * occurs.
 */
public final class CompilerFactory {

	private CompilerFactory() {
	}

	/**
	 * Build a compiler from the given configuration.
	 *
	 * @param config
	 *            the compilation configuration.
	 * @param stdout
	 *            stream to use when the output target is
	 *            {@link CompilationConfig#STDOUT}.
	 * @return a ready-to-use {@link Compiler}.
	 * @throws UnsupportedOperationException
	 *             if the requested mode or format is not yet implemented.
	 */
	public static Compiler build(CompilationConfig config,
			OutputStream stdout) {
		return switch (config.mode()) {
			case DIAGNOSTIC -> buildDiagnosticCompiler(config, stdout);
			case PROCESS -> buildProcessCompiler(config, stdout);
		};
	}

	// -------------------------------------------------------------------------
	// Reusable sub-chains (also exposed for embedding in larger pipelines)
	// -------------------------------------------------------------------------

	/**
	 * Parsing chain: reads a source file, lexes, parses, and extracts the
	 * action list. Aborts on any syntax error via {@link HaltAndCatchFire}.
	 */
	public static ChainBuilder<InputStream, List<Command>> parsingChain() {
		return new FileSource().andThen(new CharStreamProvider())
				.andThen(new Lexer()).andThen(new Parser())
				.andThen(new HaltAndCatchFire<ParseTree>())
				.andThen(new ActionListProvider());
	}

	/**
	 * Full unit builder: extends the parsing chain with model construction.
	 *
	 * <p>
	 * TODO: port {@code CompletenessChecker} and {@code ConsistencyChecker}.
	 */
	public static Transformation<List<Command>, Unit> unitBuilder() {
		return new ActionListInterpretation();
	}

	// -------------------------------------------------------------------------
	// Mode builders
	// -------------------------------------------------------------------------

	private static Compiler buildDiagnosticCompiler(CompilationConfig config,
			OutputStream stdout) {
		return parsingChain().andThen(unitBuilder())
				.andThen(new DiagnosticReport())
				.andThen(new StringSink(stdout));
	}

	private static Compiler buildProcessCompiler(CompilationConfig config,
			OutputStream stdout) {
		return switch (config.format()) {
			case JPIPE -> parsingChain().andThen(unitBuilder())
					.andThen(new ExportToJpipe())
					.andThen(new StringSink(stdout));
			case DOT -> parsingChain().andThen(dotChain(config))
					.andThen(new StringSink(stdout));
			case PNG -> parsingChain().andThen(dotChain(config))
					.andThen(new RenderWithDot("png"))
					.andThen(new ByteSink(stdout));
			case JPEG -> parsingChain().andThen(dotChain(config))
					.andThen(new RenderWithDot("jpeg"))
					.andThen(new ByteSink(stdout));
			case SVG -> parsingChain().andThen(dotChain(config))
					.andThen(new RenderWithDot("svg"))
					.andThen(new ByteSink(stdout));
			case JSON -> parsingChain().andThen(unitBuilder())
					.andThen(new SelectModel(config.diagramName()))
					.andThen(new ExportToJson())
					.andThen(new StringSink(stdout));
			case PYTHON -> parsingChain().andThen(unitBuilder())
					.andThen(new SelectModel(config.diagramName()))
					.andThen(new ExportToPython())
					.andThen(new StringSink(stdout));
			default -> throw new UnsupportedOperationException(
					"Format not yet supported: " + config.format());
		};
	}

	private static Transformation<List<Command>, String> dotChain(
			CompilationConfig config) {
		return unitBuilder().andThen(new SelectModel(config.diagramName()))
				.andThen(new ExportToDot());
	}
}
