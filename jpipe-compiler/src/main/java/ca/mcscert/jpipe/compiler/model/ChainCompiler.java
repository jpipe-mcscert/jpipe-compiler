package ca.mcscert.jpipe.compiler.model;

import ca.mcscert.jpipe.compiler.Compiler;
import java.io.Closeable;
import java.io.IOException;
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

	ChainCompiler(Source<I> source, Transformation<I, O> chain, Sink<O> sink) {
		this.source = source;
		this.chain = chain;
		this.sink = sink;
	}

	@Override
	public boolean compile(String sourceFile, String sinkFile)
			throws IOException {
		logger.info("Compiling [{}]", sourceFile);
		CompilationContext ctx = new CompilationContext(sourceFile);
		I input = source.provideFrom(sourceFile);
		try {
			O output = chain.fire(input, ctx);
			sink.pourInto(output);
		} finally {
			release(input);
			printDiagnostics(ctx);
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

	private void printDiagnostics(CompilationContext ctx) {
		for (Diagnostic d : ctx.diagnostics()) {
			if (d.isError()) {
				String loc = d.hasLocation()
						? d.source() + ":" + d.line() + ":" + d.column() + ": "
						: d.source() + ": ";
				String level = d.level().name().toLowerCase();
				String msg = d.hasCode()
						? "[" + d.code() + "] " + d.message()
						: d.message();
				logger.error("{}{}: {}", loc, level, msg);
			}
		}
	}

}
