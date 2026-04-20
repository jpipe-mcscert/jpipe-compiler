package ca.mcscert.jpipe.compiler.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ChainBuilderTest {

	@Test
	void asTransformation_applies_single_step() {
		Source<String> source = Source.of(name -> name);
		Transformation<String, Integer> step = Transformation
				.of((s, ctx) -> s.length());
		CompilationContext ctx = new CompilationContext("test");

		int result = source.andThen(step).asTransformation().fire("hello", ctx);

		assertThat(result).isEqualTo(5);
	}

	@Test
	void andThen_chains_multiple_transformations() {
		Source<String> source = Source.of(name -> name);
		CompilationContext ctx = new CompilationContext("test");

		int result = source.andThen(Transformation.of((s, c) -> s.length()))
				.andThen(Transformation.of((n, c) -> n * 2)).asTransformation()
				.fire("hello", ctx);

		assertThat(result).isEqualTo(10);
	}

	@Test
	void andThen_sink_produces_chain_compiler() {
		Source<String> source = Source.of(name -> name);
		Transformation<String, Integer> step = Transformation
				.of((s, ctx) -> s.length());
		Sink<Integer> sink = n -> {
		};

		ChainCompiler<String, Integer> compiler = source.andThen(step)
				.andThen(sink);

		assertThat(compiler).isNotNull();
	}
}
