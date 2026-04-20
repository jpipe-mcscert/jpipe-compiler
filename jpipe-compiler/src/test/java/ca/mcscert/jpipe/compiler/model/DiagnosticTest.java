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

}
