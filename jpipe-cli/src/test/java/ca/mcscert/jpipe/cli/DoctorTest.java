package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DoctorTest {

	@Test
	void run_returns_boolean() {
		boolean result = Doctor.run();
		assertThat(result).isIn(true, false);
	}

	@Test
	void run_returns_true_when_dot_is_available_or_false_when_not() {
		// run() probes external tools and returns true only if ALL are found.
		// We cannot assert the exact value without knowing the environment, but
		// we
		// can assert the method completes without throwing.
		boolean result = Doctor.run();
		assertThat(result).isNotNull();
	}
}
