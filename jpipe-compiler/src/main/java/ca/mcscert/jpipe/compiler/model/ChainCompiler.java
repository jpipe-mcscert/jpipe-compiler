package ca.mcscert.jpipe.compiler.model;

import ca.mcscert.jpipe.compiler.Compiler;
import java.io.IOException;
import java.io.PrintStream;

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

	private final Source<I> source;
	private final Transformation<I, O> chain;
	private final Sink<O> sink;

	ChainCompiler(Source<I> source, Transformation<I, O> chain, Sink<O> sink) {
		this.source = source;
		this.chain = chain;
		this.sink = sink;
	}

	@Override
	public void compile(String sourceFile, String sinkFile) throws IOException {
		CompilationContext ctx = new CompilationContext(sourceFile);
		I input = source.provideFrom(sourceFile);
		try {
			O output = chain.fire(input, ctx);
			sink.pourInto(output);
		} finally {
			printDiagnostics(ctx, System.err);
		}
	}

	private void printDiagnostics(CompilationContext ctx, PrintStream err) {
		for (Diagnostic d : ctx.diagnostics()) {
			if (d.isError()) {
				String loc = d.hasLocation()
						? d.source() + ":" + d.line() + ":" + d.column() + ": "
						: d.source() + ": ";
				err.println(loc + d.level().name().toLowerCase() + ": "
						+ d.message());
			}
		}
	}

}
