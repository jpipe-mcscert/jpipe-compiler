package ca.mcscert.jpipe.compiler.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.model.SourceLocation;
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
		var diagnostics = ctx.diagnostics();
		var entry = Diagnostic.error("x", "y");
		assertThatThrownBy(() -> diagnostics.add(entry))
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void diagnostics_preservesReportOrder() {
		ctx.error("first");
		ctx.fatal("second");
		assertThat(ctx.diagnostics()).extracting(Diagnostic::message)
				.containsExactly("first", "second");
	}

	@Test
	void codedError_carriesTheCodeAndTheSourcePath() {
		ctx.error(DiagnosticCodes.UNKNOWN_MODEL, "unknown model 'foo'");

		Diagnostic d = ctx.diagnostics().get(0);
		assertThat(d.code()).isEqualTo("unknown-model");
		assertThat(d.source()).isEqualTo("file.jd");
		assertThat(d.message()).isEqualTo("unknown model 'foo'");
		assertThat(d.hasLocation()).isFalse();
	}

	@Test
	void codedErrorWithLocation_carriesEverything() {
		ctx.error(DiagnosticCodes.UNKNOWN_ELEMENT, 7, 3, "unknown element 'e'");

		Diagnostic d = ctx.diagnostics().get(0);
		assertThat(d.code()).isEqualTo("unknown-element");
		assertThat(d.line()).isEqualTo(7);
		assertThat(d.column()).isEqualTo(3);
		assertThat(d.message()).isEqualTo("unknown element 'e'");
	}

	@Test
	void uncodedError_hasNoCode() {
		ctx.error(7, 3, "plain message");

		assertThat(ctx.diagnostics().get(0).hasCode()).isFalse();
	}

	@Test
	void errorAtAKnownLocation_carriesItsLineAndColumn() {
		ctx.error("some-rule", new SourceLocation(12, 4), "broken");

		assertThat(ctx.diagnostics()).singleElement()
				.extracting(Diagnostic::level, Diagnostic::code,
						Diagnostic::line, Diagnostic::column,
						Diagnostic::message)
				.containsExactly(Diagnostic.Level.ERROR, "some-rule", 12, 4,
						"broken");
	}

	@Test
	void errorAtAnUnknownLocation_isReportedWithoutOne() {
		ctx.error("some-rule", SourceLocation.UNKNOWN, "broken");

		assertThat(ctx.diagnostics()).singleElement()
				.extracting(Diagnostic::code, Diagnostic::source,
						Diagnostic::line, Diagnostic::column)
				.containsExactly("some-rule", "file.jd",
						Diagnostic.UNKNOWN_LOCATION,
						Diagnostic.UNKNOWN_LOCATION);
		assertThat(ctx.hasFatalErrors()).isFalse();
	}
}
