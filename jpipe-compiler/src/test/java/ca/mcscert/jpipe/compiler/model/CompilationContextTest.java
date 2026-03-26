package ca.mcscert.jpipe.compiler.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.model.Diagnostic.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompilationContextTest {

	private CompilationContext ctx;

	@BeforeEach
	void setUp() {
		ctx = new CompilationContext("file.jd");
	}

	@Test
	void sourcePath_isPreserved() {
		assertThat(ctx.sourcePath()).isEqualTo("file.jd");
	}

	@Test
	void freshContext_hasNoErrors() {
		assertThat(ctx.hasErrors()).isFalse();
		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(ctx.diagnostics()).isEmpty();
	}

	@Test
	void warn_doesNotCountAsError() {
		ctx.warn("minor issue");
		assertThat(ctx.hasErrors()).isFalse();
		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(ctx.diagnostics()).hasSize(1);
		assertThat(ctx.diagnostics().get(0).level()).isEqualTo(Level.WARNING);
	}

	@Test
	void error_countsAsErrorButNotFatal() {
		ctx.error("something wrong");
		assertThat(ctx.hasErrors()).isTrue();
		assertThat(ctx.hasFatalErrors()).isFalse();
	}

	@Test
	void fatal_countsAsBothErrorAndFatal() {
		ctx.fatal("unrecoverable");
		assertThat(ctx.hasErrors()).isTrue();
		assertThat(ctx.hasFatalErrors()).isTrue();
	}

	@Test
	void report_acceptsPrebuiltDiagnostic() {
		ctx.report(Diagnostic.error("other.jd", "from parser"));
		assertThat(ctx.hasErrors()).isTrue();
		assertThat(ctx.diagnostics().get(0).source()).isEqualTo("other.jd");
	}

	@Test
	void diagnostics_isUnmodifiable() {
		assertThatThrownBy(() -> ctx.diagnostics().add(Diagnostic.warning("x", "y")))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void diagnostics_preservesReportOrder() {
		ctx.warn("first");
		ctx.error("second");
		ctx.fatal("third");
		assertThat(ctx.diagnostics()).extracting(Diagnostic::message).containsExactly("first", "second", "third");
	}

}
