package ca.mcscert.jpipe.commands.linking;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class LinkingCommandsTest {

	@Nested
	class AddElementTest {

		@Test
		void adds_element_to_container() throws Exception {
			Unit unit = unitWithJustification("j");
			Evidence e = new Evidence("e1", "E");

			new AddElement("j", e).execute(unit);

			assertThat(unit.get("j").findById("e1")).isPresent();
		}

		@Test
		void condition_is_false_when_container_missing() {
			assertThat(new AddElement("missing", new Evidence("e", "E"))
					.condition().test(new Unit("src"))).isFalse();
		}

		@Test
		void condition_is_true_when_container_exists() {
			Unit unit = unitWithJustification("j");
			assertThat(new AddElement("j", new Evidence("e", "E")).condition()
					.test(unit)).isTrue();
		}

		@Test
		void toString_contains_container_and_element_id() {
			String s = new AddElement("j", new Evidence("e1", "E")).toString();
			assertThat(s).contains("j").contains("e1");
		}
	}

	@Nested
	class RemoveElementTest {

		@Test
		void removes_element_from_container() throws Exception {
			Unit unit = new Unit("src");
			Justification j = new Justification("j");
			j.addElement(new Evidence("e1", "E"));
			unit.add(j);

			new RemoveElement("j", "e1").execute(unit);

			assertThat(unit.get("j").findById("e1")).isEmpty();
		}

		@Test
		void condition_is_false_when_element_absent() {
			Unit unit = unitWithJustification("j");
			assertThat(new RemoveElement("j", "missing").condition().test(unit))
					.isFalse();
		}

		@Test
		void condition_is_true_when_element_present() {
			Unit unit = new Unit("src");
			Justification j = new Justification("j");
			j.addElement(new Evidence("e1", "E"));
			unit.add(j);
			assertThat(new RemoveElement("j", "e1").condition().test(unit))
					.isTrue();
		}

		@Test
		void toString_contains_container_and_element_id() {
			String s = new RemoveElement("j", "e1").toString();
			assertThat(s).contains("j").contains("e1");
		}
	}

	@Nested
	class RewireStrategySupportTest {

		@Test
		void replaces_old_supporter_with_new_one() throws Exception {
			Unit unit = new Unit("src");
			Justification j = new Justification("j");
			Strategy s = new Strategy("s", "S");
			Evidence oldE = new Evidence("e_old", "Old");
			Evidence newE = new Evidence("e_new", "New");
			j.addElement(s);
			j.addElement(oldE);
			j.addElement(newE);
			s.addSupport(oldE);
			unit.add(j);

			new RewireStrategySupport("j", "s", "e_old", "Evidence", "e_new",
					"Evidence").execute(unit);

			assertThat(s.getSupports()).containsExactly(newE);
		}

		@Test
		void condition_is_true_when_all_elements_present() {
			Unit unit = new Unit("src");
			Justification j = new Justification("j");
			Strategy s = new Strategy("s", "S");
			Evidence oldE = new Evidence("e_old", "Old");
			Evidence newE = new Evidence("e_new", "New");
			j.addElement(s);
			j.addElement(oldE);
			j.addElement(newE);
			unit.add(j);

			assertThat(new RewireStrategySupport("j", "s", "e_old", "Evidence",
					"e_new", "Evidence").condition().test(unit)).isTrue();
		}

		@Test
		void toString_contains_ids() {
			String s = new RewireStrategySupport("j", "s", "e_old", "Evidence",
					"e_new", "Evidence").toString();
			assertThat(s).contains("j").contains("s").contains("e_old")
					.contains("e_new");
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static Unit unitWithJustification(String name) {
		Unit unit = new Unit("src");
		unit.add(new Justification(name));
		return unit;
	}
}
