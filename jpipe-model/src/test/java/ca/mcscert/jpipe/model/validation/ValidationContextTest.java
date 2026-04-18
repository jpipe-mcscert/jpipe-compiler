package ca.mcscert.jpipe.model.validation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import org.junit.jupiter.api.Test;

class ValidationContextTest {

	@Test
	void standalone_locationOf_model_returns_unknown() {
		assertThat(ValidationContext.STANDALONE.locationOf("any-model"))
				.isEqualTo(SourceLocation.UNKNOWN);
	}

	@Test
	void standalone_locationOf_element_returns_unknown() {
		assertThat(ValidationContext.STANDALONE.locationOf("any-model",
				"any-element")).isEqualTo(SourceLocation.UNKNOWN);
	}

	@Test
	void unit_backed_context_resolves_model_location() {
		Unit unit = new Unit("src");
		unit.add(new Justification("j"));
		SourceLocation loc = new SourceLocation(3, 0);
		unit.recordLocation("j", loc);

		assertThat(ValidationContext.of(unit).locationOf("j")).isEqualTo(loc);
	}

	@Test
	void unit_backed_context_resolves_element_location() {
		Unit unit = new Unit("src");
		unit.add(new Justification("j"));
		SourceLocation loc = new SourceLocation(5, 2);
		unit.recordLocation("j", "e1", loc);

		assertThat(ValidationContext.of(unit).locationOf("j", "e1"))
				.isEqualTo(loc);
	}

	@Test
	void unit_backed_context_returns_unknown_for_unregistered_model() {
		Unit unit = new Unit("src");
		unit.add(new Justification("j"));

		assertThat(ValidationContext.of(unit).locationOf("j"))
				.isEqualTo(SourceLocation.UNKNOWN);
	}
}
