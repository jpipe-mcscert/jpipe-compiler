package ca.mcscert.jpipe.model.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.Violation;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompletenessValidatorTest {

	private final CompletenessValidator validator = new CompletenessValidator();

	@Nested
	class ValidModels {

		@Test
		void complete_justification_has_no_violations() {
			assertThat(validator.validateModel(simpleJustification()))
					.isEmpty();
		}

		@Test
		void complete_template_has_no_violations() {
			assertThat(validator.validateModel(simpleTemplate())).isEmpty();
		}
	}

	@Nested
	class ConclusionPresent {

		@Test
		void model_without_conclusion_produces_violation() {
			Justification j = new Justification("j");

			List<Violation> violations = validator.validateModel(j);

			assertThat(violations).extracting(Violation::rule)
					.contains("conclusion-present");
		}
	}

	@Nested
	class ConclusionSupported {

		@Test
		void conclusion_with_no_strategy_produces_violation() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "C"));

			List<Violation> violations = validator.validateModel(j);

			assertThat(violations).extracting(Violation::rule)
					.contains("conclusion-supported");
		}
	}

	@Nested
	class StrategySupported {

		@Test
		void strategy_with_no_supports_produces_violation() {
			Justification j = new Justification("j");
			Conclusion c = new Conclusion("c", "C");
			Strategy s = new Strategy("s", "S");
			j.setConclusion(c);
			j.addElement(s);
			c.addSupport(s); // s has no supports

			List<Violation> violations = validator.validateModel(j);

			assertThat(violations).extracting(Violation::rule)
					.contains("strategy-supported");
		}
	}

	@Nested
	class SubConclusionSupported {

		@Test
		void sub_conclusion_with_no_strategy_produces_violation() {
			Justification j = new Justification("j");
			Conclusion c = new Conclusion("c", "C");
			Strategy s = new Strategy("s", "S");
			Evidence e = new Evidence("e", "E");
			SubConclusion sc = new SubConclusion("sc", "SC");
			j.setConclusion(c);
			j.addElement(s);
			j.addElement(e);
			j.addElement(sc);
			c.addSupport(s);
			s.addSupport(e);
			// sc has no support

			List<Violation> violations = validator.validateModel(j);

			assertThat(violations).extracting(Violation::rule)
					.contains("sub-conclusion-supported");
		}
	}

	@Nested
	class JustificationSpecificRules {

		@Test
		void unoverridden_abstract_support_produces_violation() {
			Template t = new Template("t");
			Conclusion tc = new Conclusion("c", "C");
			Strategy ts = new Strategy("s", "S");
			AbstractSupport abs = new AbstractSupport("abs", "A");
			t.setConclusion(tc);
			t.addElement(ts);
			t.addElement(abs);
			ts.addSupport(abs);
			tc.addSupport(ts);

			Justification j = new Justification("j");
			j.inline(t, "t"); // t:abs is inlined but never overridden

			List<Violation> violations = validator.validateModel(j);

			assertThat(violations).extracting(Violation::rule)
					.contains("no-abstract-support");
		}
	}

	@Nested
	class TemplateSpecificRules {

		@Test
		void template_without_abstract_support_produces_violation() {
			Template t = new Template("t");
			Conclusion c = new Conclusion("c", "C");
			Strategy s = new Strategy("s", "S");
			Evidence e = new Evidence("e", "E");
			t.setConclusion(c);
			t.addElement(s);
			t.addElement(e);
			s.addSupport(e);
			c.addSupport(s);

			List<Violation> violations = validator.validateModel(t);

			assertThat(violations).extracting(Violation::rule)
					.contains("has-abstract-support");
		}
	}

	@Nested
	class UnitValidation {

		@Test
		void validate_unit_checks_all_models() {
			Unit unit = new Unit("src");
			unit.add(new Justification("j1")); // no conclusion
			unit.add(new Justification("j2")); // no conclusion

			List<Violation> violations = validator.validate(unit);

			assertThat(violations).hasSize(2).extracting(Violation::rule)
					.containsOnly("conclusion-present");
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static Justification simpleJustification() {
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "The system is correct");
		Strategy s = new Strategy("s", "Testing");
		Evidence e = new Evidence("e1", "Test results");
		j.setConclusion(c);
		j.addElement(s);
		j.addElement(e);
		s.addSupport(e);
		c.addSupport(s);
		return j;
	}

	private static Template simpleTemplate() {
		Template t = new Template("t");
		Conclusion c = new Conclusion("c", "Conclusion");
		Strategy s = new Strategy("s", "Strategy");
		AbstractSupport abs = new AbstractSupport("abs", "Abstract step");
		t.setConclusion(c);
		t.addElement(s);
		t.addElement(abs);
		s.addSupport(abs);
		c.addSupport(s);
		return t;
	}
}
