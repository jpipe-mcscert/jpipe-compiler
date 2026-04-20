package ca.mcscert.jpipe.operators;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.operators.builtin.RefineOperator;
import org.junit.jupiter.api.Test;

class OperatorRegistryTest {

	@Test
	void registered_operator_is_found_by_name() {
		OperatorRegistry registry = new OperatorRegistry();
		CompositionOperator op = new RefineOperator();
		registry.register("refine", op);

		assertThat(registry.find("refine")).isPresent().get().isSameAs(op);
	}

	@Test
	void unknown_name_returns_empty() {
		OperatorRegistry registry = new OperatorRegistry();
		assertThat(registry.find("unknown")).isEmpty();
	}

	@Test
	void registered_names_contains_all_registered_keys() {
		OperatorRegistry registry = new OperatorRegistry();
		registry.register("refine", new RefineOperator());
		registry.register("assemble", new RefineOperator());

		assertThat(registry.registeredNames()).containsExactly("refine",
				"assemble");
	}

	@Test
	void later_registration_overwrites_earlier_under_same_name() {
		OperatorRegistry registry = new OperatorRegistry();
		CompositionOperator first = new RefineOperator();
		CompositionOperator second = new RefineOperator();
		registry.register("refine", first);
		registry.register("refine", second);

		assertThat(registry.find("refine")).isPresent().get().isSameAs(second);
	}
}
