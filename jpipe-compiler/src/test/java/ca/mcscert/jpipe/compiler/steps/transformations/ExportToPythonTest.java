package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.elements.Evidence;
import org.junit.jupiter.api.Test;

class ExportToPythonTest {

	@Test
	void exports_a_model_to_python() {
		Justification j = new Justification("j");
		j.addElement(new Evidence("e", "An evidence"));

		String python = new ExportToPython().fire(j,
				new CompilationContext("file.jd"));

		assertThat(python).contains("def an_evidence(");
	}

	@Test
	void an_id_bound_to_two_functions_fails_the_step_rather_than_escaping() {
		// Consistency reports the duplicate id as a non-fatal error, so the
		// export still runs: its refusal must end the compilation cleanly,
		// not reach the user as a stack trace (#182).
		Justification j = new Justification("j");
		j.addElement(new Evidence("dup", "First proof"));
		j.addElement(new Evidence("dup", "Second proof"));
		ExportToPython step = new ExportToPython();
		CompilationContext ctx = new CompilationContext("file.jd");

		assertThatThrownBy(() -> step.fire(j, ctx))
				.isInstanceOf(CompilationException.class)
				.hasMessageContainingAll("ExportToPython", "\"j:dup\"");
	}
}
