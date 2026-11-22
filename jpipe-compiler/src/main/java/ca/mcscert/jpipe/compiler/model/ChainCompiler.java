package ca.mcscert.jpipe.compiler.model;

import ca.mcscert.jpipe.compiler.Compiler;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Objects;
import java.util.function.Consumer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A fully assembled compilation pipeline: {@link Source} →
 * {@link Transformation} chain → {@link Sink}. Produced by
 * {@link ChainBuilder#andThen(Sink)}.
 *
 * @param <I>
 *            type provided by the source.
 * @param <O>
 *            type consumed by the sink.
 */
public final class ChainCompiler<I, O> implements Compiler {

	private static final Logger logger = LogManager.getLogger();

	private final Source<I> source;
	private final Transformation<I, O> chain;
	private final Sink<O> sink;
	private final Consumer<CompilationContext> report;

	ChainCompiler(Source<I> source, Transformation<I, O> chain, Sink<O> sink) {
		this(source, chain, sink, DiagnosticPrinter::print);
	}

	private ChainCompiler(Source<I> source, Transformation<I, O> chain,
			Sink<O> sink, Consumer<CompilationContext> report) {
		this.source = source;
		this.chain = chain;
		this.sink = sink;
		this.report = report;
	}

	/**
	 * The same compiler, printing its diagnostics on {@code diagnostics}
	 * instead of the {@code jpipe.diagnostics} logger. The lines are the same;
	 * only where they go changes, so that a caller such as the CLI does not
	 * have to start a logging back end to show them (#216).
	 *
	 * @param diagnostics
	 *            where the ERROR and FATAL diagnostics are printed, flushed
	 *            after each compilation. Not closed.
	 * @return a new compiler; this one is unchanged.
	 */
	public ChainCompiler<I, O> reportingTo(PrintWriter diagnostics) {
		Objects.requireNonNull(diagnostics, "diagnostics");
		return new ChainCompiler<>(source, chain, sink,
				ctx -> DiagnosticPrinter.print(ctx, diagnostics));
	}

	@Override
	public boolean compile(String sourceFile, String sinkFile)
			throws IOException {
		logger.info("Compiling [{}]", sourceFile);
		CompilationContext ctx = new CompilationContext(sourceFile);
		I input = source.provideFrom(sourceFile);
		try {
			O output = chain.fire(input, ctx);
			// The last step may have recorded a fatal and still returned: what
			// it returned is no artefact to write (#180).
			if (ctx.hasFatalErrors()) {
				throw new CompilationException("ChainCompiler",
						"compilation aborted: nothing was written");
			}
			sink.pourInto(output);
		} finally {
			release(input);
			report.accept(ctx);
		}
		logger.info("Compilation finished [{}]", sourceFile);
		return ctx.hasErrors();
	}

	/**
	 * Closes the pipeline's input if it holds a resource, leaving
	 * {@link System#in} open: the pipeline borrows standard input rather than
	 * owning it, and closing it would break any later read. A source value that
	 * is not {@link Closeable} needs no release.
	 *
	 * <p>
	 * A failure to close is logged rather than raised, so it cannot mask the
	 * outcome the compilation was run to produce.
	 */
	private void release(I input) {
		if (!(input instanceof Closeable closeable) || input == System.in) {
			return;
		}
		try {
			closeable.close();
		} catch (IOException e) {
			logger.warn("Could not close the source: {}", e.getMessage());
		}
	}

}
