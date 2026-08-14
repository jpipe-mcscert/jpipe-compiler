package ca.mcscert.jpipe.compiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Sink;
import ca.mcscert.jpipe.compiler.model.Source;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.compiler.steps.transformations.DiagnosticReport;
import ca.mcscert.jpipe.model.Unit;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class DiagnosticCompilerTest {

	private final StringBuilder poured = new StringBuilder();

	// ── stubs ────────────────────────────────────────────────────────────────

	/** A source that never touches the filesystem. */
	private static Source<InputStream> source() {
		return new Source<>() {
			@Override
			public InputStream provideFrom(String path) {
				return new ByteArrayInputStream(
						"ignored".getBytes(StandardCharsets.UTF_8));
			}
		};
	}

	/** An analysis step that reports {@code diagnostic} and returns a unit. */
	private static Transformation<InputStream, Unit> analysis(
			java.util.function.Consumer<CompilationContext> diagnostic) {
		return new Transformation<>() {
			@Override
			protected Unit run(InputStream in, CompilationContext ctx) {
				diagnostic.accept(ctx);
				return new Unit(ctx.sourcePath());
			}
		};
	}

	private Sink<String> sink() {
		return poured::append;
	}

	private DiagnosticCompiler compiler(
			Transformation<InputStream, Unit> analysis) {
		return new DiagnosticCompiler(source(), analysis,
				new DiagnosticReport(), sink());
	}

	// ── tests ────────────────────────────────────────────────────────────────

	@Test
	void aCleanCompilationIsReportedAndSucceeds() throws IOException {
		boolean hasErrors = compiler(analysis(ctx -> {
			// intentionally quiet: nothing to report
		})).compile("clean.jd", "<stdout>");

		assertThat(hasErrors).isFalse();
		assertThat(poured).contains("=== Diagnostics ===").contains("(none)");
	}

	@Test
	void anAbortedCompilationIsStillReported() throws IOException {
		// The fatal makes the *next* fire() boundary throw, which is exactly
		// the case that used to produce no report at all (#154).
		boolean hasErrors = compiler(analysis(ctx -> ctx.fatal("unrecoverable"))
				.andThen(passThrough())).compile("broken.jd", "<stdout>");

		assertThat(hasErrors).isTrue();
		assertThat(poured).contains("[FATAL]").contains("unrecoverable")
				.contains("(empty)");
	}

	@Test
	void aBugInAStepIsNotSwallowed() {
		Transformation<InputStream, Unit> exploding = new Transformation<>() {
			@Override
			protected Unit run(InputStream in, CompilationContext ctx) {
				throw new IllegalStateException("a bug, not a diagnostic");
			}
		};
		DiagnosticCompiler compiler = compiler(exploding);

		assertThatThrownBy(() -> compiler.compile("any.jd", "<stdout>"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("a bug");
	}

	/** A step that only exists to give the fatal a boundary to abort at. */
	private static Transformation<Unit, Unit> passThrough() {
		return new Transformation<>() {
			@Override
			protected Unit run(Unit in, CompilationContext ctx) {
				return in;
			}
		};
	}
}
