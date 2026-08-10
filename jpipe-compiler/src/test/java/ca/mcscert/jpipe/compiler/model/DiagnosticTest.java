package ca.mcscert.jpipe.compiler.model;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.compiler.model.Diagnostic.Level;
import org.junit.jupiter.api.Test;

class DiagnosticTest {

	@Test
	void error_isAnErrorButNotFatal() {
		Diagnostic d = Diagnostic.error("file.jd", "something wrong");
		assertThat(d.level()).isEqualTo(Level.ERROR);
		assertThat(d.isError()).isTrue();
		assertThat(d.isFatal()).isFalse();
	}

	@Test
	void fatal_isAnErrorAndFatal() {
		Diagnostic d = Diagnostic.fatal("file.jd", "unrecoverable");
		assertThat(d.level()).isEqualTo(Level.FATAL);
		assertThat(d.isError()).isTrue();
		assertThat(d.isFatal()).isTrue();
	}

	@Test
	void error_withoutCode_hasNoCode() {
		Diagnostic d = Diagnostic.error("file.jd", "something wrong");
		assertThat(d.code()).isNull();
		assertThat(d.hasCode()).isFalse();
	}

	@Test
	void error_withCode_keepsTheCodeOutOfTheMessage() {
		Diagnostic d = Diagnostic.error(DiagnosticCodes.UNKNOWN_MODEL,
				"file.jd", "unknown model 'foo'");
		assertThat(d.hasCode()).isTrue();
		assertThat(d.code()).isEqualTo("unknown-model");
		assertThat(d.message()).isEqualTo("unknown model 'foo'")
				.doesNotContain("unknown-model");
	}

	@Test
	void error_withCodeAndLocation_carriesBoth() {
		Diagnostic d = Diagnostic.error(DiagnosticCodes.UNKNOWN_ELEMENT,
				"file.jd", 12, 4, "unknown element 'e'");
		assertThat(d.code()).isEqualTo("unknown-element");
		assertThat(d.hasLocation()).isTrue();
		assertThat(d.line()).isEqualTo(12);
		assertThat(d.column()).isEqualTo(4);
	}

	@Test
	void diagnosticCodes_areBareKebabCase() {
		assertThat(DiagnosticCodes.UNKNOWN_MODEL).doesNotContain("[")
				.doesNotContain("]").isEqualTo("unknown-model");
	}

	@Test
	void emptyCode_countsAsNoCode() {
		Diagnostic d = Diagnostic.error("", "file.jd", "message");
		assertThat(d.hasCode()).isFalse();
	}

	@Test
	void fatal_neverCarriesACode() {
		Diagnostic d = Diagnostic.fatal("file.jd", 1, 2, "unrecoverable");
		assertThat(d.hasCode()).isFalse();
	}

}
