package ca.mcscert.jpipe.compiler.steps.checkers;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsistencyCheckerTest {

	private CompilationContext ctx;

	@BeforeEach
	void setUp() {
		ctx = new CompilationContext("file.jd");
	}

	@Test
	void valid_unit_produces_no_diagnostics() {
		new ConsistencyChecker().fire(validUnit(), ctx);

		assertThat(ctx.hasErrors()).isFalse();
	}

	@Test
	void unit_returned_unchanged() {
		Unit unit = validUnit();
		assertThat(new ConsistencyChecker().fire(unit, ctx)).isSameAs(unit);
	}

	@Test
	void duplicate_element_ids_produce_error_diagnostic() {
		Unit unit = new Unit("src");
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "C"));
		j.addElement(new Strategy("s", "S1"));
		j.addElement(new Strategy("s", "S2")); // duplicate id
		unit.add(j);

		new ConsistencyChecker().fire(unit, ctx);

		assertThat(ctx.hasErrors()).isTrue();
		assertThat(ctx.hasFatalErrors()).isFalse();
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static Unit validUnit() {
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
