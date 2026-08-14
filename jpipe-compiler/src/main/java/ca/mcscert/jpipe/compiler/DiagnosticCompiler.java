package ca.mcscert.jpipe.compiler;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.compiler.model.Sink;
import ca.mcscert.jpipe.compiler.model.Source;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.compiler.steps.transformations.CollectDiagnostics;
import ca.mcscert.jpipe.compiler.steps.transformations.DiagnosticRenderer;
import ca.mcscert.jpipe.model.Unit;
import java.io.IOException;
import java.io.InputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The compiler behind the {@code diagnostic} command: it reports on a
 * compilation rather than producing an artefact from it.
 *
 * <p>
 * That distinction is why this is not a {@link ChainCompiler
 * ca.mcscert.jpipe.compiler.model.ChainCompiler}. A fatal diagnostic aborts the
 * pipeline at the next {@code fire()} boundary (ADR-0016), so a report
 * assembled as the last steps of that same pipeline could never describe a
 * syntax error or an unresolvable {@code load} — precisely the failures a tool
 * most needs to read. Here the analysis runs as a pipeline, and the report is
 * written whether it completed or aborted; when it aborted, on the empty unit
 * it never got to build.
 *
 * <p>
 * The pipeline invariant is untouched: no step runs after a fatal, and
 * {@code process} still fails without writing anything, since a report poured
 * into an export stream would corrupt the artefact.
 */
final class DiagnosticCompiler implements Compiler {

	private static final Logger logger = LogManager.getLogger();

	private final Source<InputStream> source;
	private final Transformation<InputStream, Unit> analysis;
	private final DiagnosticRenderer renderer;
	private final Sink<String> sink;

	DiagnosticCompiler(Source<InputStream> source,
			Transformation<InputStream, Unit> analysis,
			DiagnosticRenderer renderer, Sink<String> sink) {
		this.source = source;
		this.analysis = analysis;
		this.renderer = renderer;
		this.sink = sink;
	}

	@Override
	public boolean compile(String sourceFile, String sinkFile)
			throws IOException {
		logger.info("Compiling [{}]", sourceFile);
		CompilationContext ctx = new CompilationContext(sourceFile);
		InputStream input = source.provideFrom(sourceFile);
		Unit unit;
		try {
			unit = analysis.fire(input, ctx);
		} catch (CompilationException _) {
			// The abort is the subject of the report, not a reason to skip it.
			// Only a fatal is caught here: a bug in a step still propagates.
			unit = new Unit(sourceFile);
		}
		sink.pourInto(
				renderer.render(new CollectDiagnostics().snapshot(unit, ctx)));
		logger.info("Compilation finished [{}]", sourceFile);
		return ctx.hasErrors();
	}
}
