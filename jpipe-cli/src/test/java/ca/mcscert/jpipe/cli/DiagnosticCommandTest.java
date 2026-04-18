package ca.mcscert.jpipe.cli;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.compiler.CompilationConfig;
import ca.mcscert.jpipe.compiler.model.CompilationException;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import org.junit.jupiter.api.Test;

class DiagnosticCommandTest {

	@Test
	void doCall_valid_file_returns_exit_ok() throws Exception {
		DiagnosticCommand cmd = new DiagnosticCommand();
		cmd.input = resourcePath("test_minimal.jd");
		cmd.output = CompilationConfig.STDOUT;

		Integer result = cmd.doCall(new ByteArrayOutputStream());

		assertThat(result).isEqualTo(Main.EXIT_OK);
	}

	@Test
	void doCall_invalid_syntax_throws_compilation_exception() {
		DiagnosticCommand cmd = new DiagnosticCommand();
		cmd.input = resourcePath("test_invalid.jd");
		cmd.output = CompilationConfig.STDOUT;

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertThatThrownBy(() -> cmd.doCall(out))
				.isInstanceOf(CompilationException.class);
	}

	private static String resourcePath(String name) {
		URL url = DiagnosticCommandTest.class.getClassLoader()
				.getResource(name);
		if (url == null) {
			throw new IllegalStateException("Test resource not found: " + name);
		}
		return url.getPath();
	}
}
