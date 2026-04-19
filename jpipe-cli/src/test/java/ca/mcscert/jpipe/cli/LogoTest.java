package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class LogoTest {

	@Test
	void sout_prints_banner_to_stdout() {
		PrintStream original = System.out;
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		System.setOut(new PrintStream(buffer));
		try {
			Logo.sout();
		} finally {
			System.setOut(original);
		}
		assertThat(buffer.toString()).contains("McSCert");
	}
}
