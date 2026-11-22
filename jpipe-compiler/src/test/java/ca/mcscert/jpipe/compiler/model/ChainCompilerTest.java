package ca.mcscert.jpipe.compiler.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.Compiler;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChainCompilerTest {

	/** Source that ignores the file path and returns a fixed value. */
	private static <I> Source<I> fixedSource(I value) {
		return Source.of(sourceName -> value);
	}

	/** Sink that captures whatever it receives. */
	private static <O> AtomicReference<O> capturingSink(Sink<O>[] holder) {
		AtomicReference<O> captured = new AtomicReference<>();
		holder[0] = captured::set;
		return captured;
	}

	@Test
	void reportingTo_printsTheErrorsOnTheGivenWriter() throws IOException {
		// The CLI prints diagnostics this way, without a logger (#216): the
		// lines must stay those tools parse.
		StringWriter err = new StringWriter();
		Compiler compiler = fixedSource("x")
				.andThen(Transformation.<String, String>of((input, ctx) -> {
					ctx.error("E1", 3, 7, "boom");
					return input;
				})).andThen((String output) -> {
					// intentionally empty
				}).reportingTo(new PrintWriter(err));

		boolean hasErrors = compiler.compile("f.jd", "out.txt");

		assertThat(hasErrors).isTrue();
		assertThat(err).hasToString(
				"f.jd:3:7: error: [E1] boom" + System.lineSeparator());
	}

	@Test
	@SuppressWarnings("unchecked")
	void compile_flowsValueFromSourceThroughChainToSink() throws IOException {
		Sink<Integer>[] sinkHolder = new Sink[1];
		AtomicReference<Integer> captured = capturingSink(sinkHolder);

		Compiler compiler = Source.of(sourceName -> "hello")
				.andThen(Transformation.of((input, ctx) -> input.length()))
				.andThen(sinkHolder[0]);

		compiler.compile("input.jd", "output.txt");

		assertThat(captured.get()).isEqualTo(5);
	}

	@Test
	@SuppressWarnings("unchecked")
	void compile_contextSourcePathMatchesSourceFile() throws IOException {
		AtomicReference<String> capturedPath = new AtomicReference<>();
		Sink<String>[] sinkHolder = new Sink[1];
		sinkHolder[0] = output -> {
		};

		Compiler compiler = fixedSource("x")
				.andThen(Transformation.of((input, ctx) -> {
					capturedPath.set(ctx.sourcePath());
					return input;
				})).andThen(sinkHolder[0]);

		compiler.compile("my/source.jd", "out.txt");

		assertThat(capturedPath.get()).isEqualTo("my/source.jd");
	}

	@Test
	@SuppressWarnings("unchecked")
	void compile_multipleTransformationsComposeCorrectly() throws IOException {
		Sink<String>[] sinkHolder = new Sink[1];
		AtomicReference<String> captured = capturingSink(sinkHolder);

		Compiler compiler = fixedSource("hello")
				.andThen(Transformation.of((s, ctx) -> s.toUpperCase()))
				.andThen(Transformation.of((s, ctx) -> s + "!"))
				.andThen(sinkHolder[0]);

		compiler.compile("f.jd", "out.txt");

		assertThat(captured.get()).isEqualTo("HELLO!");
	}

	@Test
	@SuppressWarnings("unchecked")
	void compile_abortWhenCheckerReportsFatal() {
		Sink<String>[] sinkHolder = new Sink[1];
		sinkHolder[0] = output -> {
		};

		Compiler compiler = fixedSource("bad")
				.andThen(Checker
						.checking((input, ctx) -> ctx.fatal("invalid input")))
				.andThen(
						Transformation.of((input, ctx) -> input + " processed"))
				.andThen(sinkHolder[0]);

		assertThatThrownBy(() -> compiler.compile("f.jd", "out.txt"))
				.isInstanceOf(CompilationException.class)
				.hasMessageContaining("fatal");
	}

	@Test
	void compile_writesNothingWhenTheLastStepRecordsAFatal() {
		// A step may record a fatal and still return: what it returned is no
		// artefact, and must not reach the sink (#180).
		AtomicReference<String> poured = new AtomicReference<>();
		Compiler compiler = fixedSource("bad")
				.andThen(Transformation.<String, String>of((input, ctx) -> {
					ctx.fatal("cannot finish");
					return "half an artefact";
				})).andThen(poured::set);

		assertThatThrownBy(() -> compiler.compile("f.jd", "out.txt"))
				.isInstanceOf(CompilationException.class)
				.hasMessageContaining("nothing was written");
		assertThat(poured.get()).isNull();
	}
}
