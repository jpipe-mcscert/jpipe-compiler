package ca.mcscert.jpipe.model.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.Violation;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConsistencyValidatorTest {

	private final ConsistencyValidator validator = new ConsistencyValidator();

	@Nested
	class NoDuplicateIds {

		@Test
		void valid_model_produces_no_violations() {
			assertThat(validator.validateModel(simpleJustification()))
					.isEmpty();
		}

		@Test
		void duplicate_element_ids_produce_violation() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "C"));
			j.addElement(new Strategy("s", "S1"));
			j.addElement(new Strategy("s", "S2")); // duplicate id

			List<Violation> violations = validator.validateModel(j);

			assertThat(violations).extracting(Violation::rule)
					.contains("no-duplicate-ids");
		}

		@Test
		void standalone_violations_carry_unknown_location() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "C"));
			j.addElement(new Strategy("s", "S1"));
			j.addElement(new Strategy("s", "S2"));

			List<Violation> violations = validator.validateModel(j);

			assertThat(violations).extracting(Violation::location)
					.containsOnly(SourceLocation.UNKNOWN);
		}

		@Test
		void unit_validate_carries_registered_location() {
			Unit unit = new Unit("src");
			Justification j = new Justification("j1");
			j.setConclusion(new Conclusion("c", "C"));
			j.addElement(new Strategy("s", "S1"));
			j.addElement(new Strategy("s", "S2"));
			unit.add(j);
			SourceLocation loc = new SourceLocation(5, 2);
			unit.recordLocation("j1", "s", loc);

			List<Violation> violations = validator.validate(unit);

			assertThat(violations).extracting(Violation::location)
					.contains(loc);
		}
	}

	@Nested
	class AcyclicSupport {

		@Test
		void cyclic_support_graph_produces_violation() {
			Justification j = new Justification("j");
			Conclusion c = new Conclusion("c", "C");
			Strategy s1 = new Strategy("s1", "S1");
			SubConclusion sc = new SubConclusion("sc", "SC");
			Strategy s2 = new Strategy("s2", "S2");

			j.setConclusion(c);
			j.addElement(s1);
			j.addElement(sc);
			j.addElement(s2);
			c.addSupport(s1);
			s1.addSupport(sc);
			sc.addSupport(s2);
			s2.addSupport(sc); // cycle: sc → s2 → sc

			List<Violation> violations = validator.validateModel(j);

			assertThat(violations).extracting(Violation::rule)
					.contains("acyclic-support");
		}

		@Test
		void acyclic_support_graph_produces_no_violation() {
			assertThat(validator.validateModel(simpleJustification()))
					.isEmpty();
		}
	}

	@Nested
	class AcyclicImplements {

		@Test
		void implements_cycle_produces_violation() {
			Template t1 = new Template("t1");
			t1.setConclusion(new Conclusion("c1", "C1"));
			Template t2 = new Template("t2");
			t2.setConclusion(new Conclusion("c2", "C2"));
			t1.inline(t2, "t2"); // t1.parent = t2
			t2.inline(t1, "t1"); // t2.parent = t1

			Unit unit = new Unit("src");
			unit.add(t1);
			unit.add(t2);

			List<Violation> violations = validator.validate(unit);

			assertThat(violations).extracting(Violation::rule)
					.contains("acyclic-implements");
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
}
