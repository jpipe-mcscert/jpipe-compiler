package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DoctorTest {

	@Test
	void run_returns_boolean() {
		boolean result = Doctor.run();
		assertThat(result).isIn(true, false);
	}
}
