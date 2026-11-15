package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.Test;

class LogoTest {

	@Test
	void print_writes_the_banner_to_stderr_and_nothing_to_stdout() {
		PrintStream originalOut = System.out;
		PrintStream originalErr = System.err;
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ByteArrayOutputStream err = new ByteArrayOutputStream();
		System.setOut(new PrintStream(out));
		System.setErr(new PrintStream(err));
		try {
			Logo.print();
		} finally {
			System.setOut(originalOut);
			System.setErr(originalErr);
		}
		assertThat(err.toString()).contains("McSCert");
		assertThat(out.toString()).isEmpty();
	}
}
