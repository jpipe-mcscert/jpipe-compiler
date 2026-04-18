package ca.mcscert.jpipe.compiler.steps.checkers;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Diagnostic;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CompletenessCheckerTest {

	private CompilationContext ctx;

	@BeforeEach
	void setUp() {
		ctx = new CompilationContext("file.jd");
	}

	@Test
	void complete_unit_produces_no_diagnostics() {
		new CompletenessChecker().fire(completeUnit(), ctx);

		assertThat(ctx.hasErrors()).isFalse();
	}

	@Test
	void unit_returned_unchanged() {
		Unit unit = completeUnit();
		assertThat(new CompletenessChecker().fire(unit, ctx)).isSameAs(unit);
	}

	@Test
	void justification_without_conclusion_produces_error_diagnostic() {
		Unit unit = new Unit("src");
		unit.add(new Justification("j")); // no conclusion

		new CompletenessChecker().fire(unit, ctx);

		assertThat(ctx.hasErrors()).isTrue();
		assertThat(ctx.hasFatalErrors()).isFalse();
	}

	@Test
	void error_diagnostic_message_contains_rule_name() {
		Unit unit = new Unit("src");
		unit.add(new Justification("j"));

		new CompletenessChecker().fire(unit, ctx);

		assertThat(ctx.diagnostics()).extracting(Diagnostic::message)
				.anyMatch(m -> m.contains("conclusion-present"));
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static Unit completeUnit() {
		Unit unit = new Unit("src");
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "The system is correct");
		Strategy s = new Strategy("s", "Testing");
		Evidence e = new Evidence("e1", "Test results");
		j.setConclusion(c);
		j.addElement(s);
		j.addElement(e);
		s.addSupport(e);
		c.addSupport(s);
		unit.add(j);
		return unit;
	}
}
