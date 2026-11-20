package ca.mcscert.jpipe.model.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.ImplementsTemplate;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.SourceLocation;
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

		@Test
		void abstract_support_supporting_no_strategy_produces_violation() {
			Template t = new Template("t");
			Conclusion c = new Conclusion("c", "C");
			Strategy s = new Strategy("s", "S");
			AbstractSupport used = new AbstractSupport("used", "Used");
			AbstractSupport dangling = new AbstractSupport("dangling", "D");
			t.setConclusion(c);
			t.addElement(s);
			t.addElement(used);
			t.addElement(dangling);
			s.addSupport(used);
			c.addSupport(s);

			List<Violation> violations = validator.validateModel(t);

			assertThat(violations).extracting(Violation::rule)
					.containsExactly("abstract-support-used");
			assertThat(violations.getFirst().message()).contains("'dangling'");
		}
	}

	@Nested
	class UnitValidation {

		@Test
		void validate_unit_skips_a_model_that_could_not_be_built() {
			// Its gaps follow from a failure reported where it happened.
			Unit unit = new Unit("src");
			unit.add(new Justification("broken")); // no conclusion
			unit.markIncomplete("broken");

			assertThat(validator.validate(unit)).isEmpty();
		}

		@Test
		void an_unused_support_is_reported_in_its_template_only() {
			// 'mid' inherits it: one mistake, reported once (#183).
			Unit unit = new ExecutionEngine().spawn("src",
					List.of(new CreateTemplate("root"),
							new CreateConclusion("root", "c", "C"),
							new CreateStrategy("root", "s", "S"),
							new CreateAbstractSupport("root", "used", "U"),
							new CreateAbstractSupport("root", "dangling", "D"),
							new AddSupport("root", "c", "s"),
							new AddSupport("root", "s", "used"),
							new CreateTemplate("mid"),
							new ImplementsTemplate("mid", "root")));

			assertThat(validator.validate(unit)).singleElement()
					.extracting(Violation::message).asString()
					.contains("'dangling' in template 'root'");
		}

		@Test
		void an_unused_support_copied_by_a_composition_is_reported_once() {
			// The copy keeps the place of the declaration it comes from.
			SourceLocation declared = new SourceLocation("src", 4, 13);
			Unit unit = new Unit("src");
			unit.add(templateWithDangling("t", "dangling"));
			unit.add(templateWithDangling("both", "unified_0"));
			unit.recordLocation("t", "dangling", declared);
			unit.recordLocation("both", "unified_0", declared);

			assertThat(validator.validate(unit)).singleElement()
					.extracting(Violation::location).isEqualTo(declared);
		}

		@Test
		void a_placeholder_whose_override_failed_is_not_reported_again() {
			Unit unit = new Unit("src");
			Justification j = simpleJustification();
			j.addElement(new AbstractSupport("t:abs", "Left behind"));
			unit.add(j);
			unit.recordFailedOverride("j", "t:abs");

			assertThat(validator.validate(unit)).isEmpty();
		}

		@Test
		void a_placeholder_whose_override_failed_in_a_template_is_spared_where_inherited() {
			// The template's failure is reported there: a model implementing
			// it is told only about the placeholders it should override.
			Unit unit = new ExecutionEngine().spawn("src",
					List.of(new CreateTemplate("t"),
							new CreateConclusion("t", "c", "C"),
							new CreateStrategy("t", "s", "S"),
							new CreateAbstractSupport("t", "abs", "Failed"),
							new CreateAbstractSupport("t", "other",
									"Unrelated"),
							new AddSupport("t", "c", "s"),
							new AddSupport("t", "s", "abs"),
							new AddSupport("t", "s", "other"),
							new CreateJustification("j"),
							new ImplementsTemplate("j", "t")));
			unit.recordFailedOverride("t", "abs");

			assertThat(validator.validate(unit)).singleElement()
					.extracting(Violation::message).asString().contains(
							"'t:other' in justification 'j' was not overridden");
		}

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

	/** A complete template, but for a @support that supports nothing. */
	private static Template templateWithDangling(String name,
			String danglingId) {
		Template t = simpleTemplate(name);
		t.addElement(new AbstractSupport(danglingId, "Dangling"));
		return t;
	}

	private static Template simpleTemplate() {
		return simpleTemplate("t");
	}

	private static Template simpleTemplate(String name) {
		Template t = new Template(name);
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
