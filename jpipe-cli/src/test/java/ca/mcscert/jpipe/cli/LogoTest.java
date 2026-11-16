package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogoTest {

	@Test
	void the_banner_names_the_project_and_its_sponsor() {
		assertThat(Logo.BANNER).contains("McSCert", "|__/");
	}
}
