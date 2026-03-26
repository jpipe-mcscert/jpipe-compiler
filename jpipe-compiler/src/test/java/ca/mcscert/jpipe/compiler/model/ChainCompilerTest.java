package ca.mcscert.jpipe.compiler.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.Compiler;
import java.io.IOException;
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
		holder[0] = (output, fileName) -> captured.set(output);
		return captured;
	}

	@Test
	@SuppressWarnings("unchecked")
	void compile_flowsValueFromSourceThroughChainToSink() throws IOException {
		Sink<Integer>[] sinkHolder = new Sink[1];
		AtomicReference<Integer> captured = capturingSink(sinkHolder);

		Compiler compiler = Source.of(sourceName -> "hello").andThen(Transformation.of((input, ctx) -> input.length()))
				.andThen(sinkHolder[0]);

		compiler.compile("input.jd", "output.txt");

		assertThat(captured.get()).isEqualTo(5);
	}

	@Test
	@SuppressWarnings("unchecked")
	void compile_contextSourcePathMatchesSourceFile() throws IOException {
		AtomicReference<String> capturedPath = new AtomicReference<>();
		Sink<String>[] sinkHolder = new Sink[1];
		sinkHolder[0] = (output, fileName) -> {
		};

		Compiler compiler = fixedSource("x").andThen(Transformation.of((input, ctx) -> {
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

		Compiler compiler = fixedSource("hello").andThen(Transformation.of((s, ctx) -> s.toUpperCase()))
				.andThen(Transformation.of((s, ctx) -> s + "!")).andThen(sinkHolder[0]);

		compiler.compile("f.jd", "out.txt");

		assertThat(captured.get()).isEqualTo("HELLO!");
	}

	@Test
	@SuppressWarnings("unchecked")
	void compile_abortWhenCheckerReportsFatal() {
		Sink<String>[] sinkHolder = new Sink[1];
		sinkHolder[0] = (output, fileName) -> {
		};

		Compiler compiler = fixedSource("bad").andThen(Checker.checking((input, ctx) -> ctx.fatal("invalid input")))
				.andThen(Transformation.of((input, ctx) -> input + " processed")).andThen(sinkHolder[0]);

		assertThatThrownBy(() -> compiler.compile("f.jd", "out.txt")).isInstanceOf(CompilationException.class)
				.hasMessageContaining("fatal");
	}

}
