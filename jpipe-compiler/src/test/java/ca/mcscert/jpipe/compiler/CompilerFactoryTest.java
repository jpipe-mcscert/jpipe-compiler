package ca.mcscert.jpipe.compiler;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CompilerFactoryTest {

	private static final PrintWriter DIAGNOSTICS = new PrintWriter(
			Writer.nullWriter());

	@Test
	void parsingChain_returns_non_null() {
		assertThat(CompilerFactory.parsingChain()).isNotNull();
	}

	@Test
	void unitBuilder_returns_non_null() {
		assertThat(CompilerFactory.unitBuilder()).isNotNull();
	}

	@ParameterizedTest
	@EnumSource(DiagnosticFormat.class)
	void buildDiagnosticCompiler_supports_every_format(
			DiagnosticFormat format) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertThat(CompilerFactory.buildDiagnosticCompiler(format, out))
				.isNotNull();
	}

	@Test
	void build_jpipe_format_returns_compiler() {
		CompilationConfig cfg = new CompilationConfig("test.jd",
				CompilationConfig.STDOUT, Format.JPIPE, "j");
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertThat(CompilerFactory.build(cfg, out, DIAGNOSTICS)).isNotNull();
	}

	@Test
	void build_dot_format_returns_compiler() {
		CompilationConfig cfg = new CompilationConfig("test.jd",
				CompilationConfig.STDOUT, Format.DOT, null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertThat(CompilerFactory.build(cfg, out, DIAGNOSTICS)).isNotNull();
	}

	@Test
	void build_json_format_returns_compiler() {
		CompilationConfig cfg = new CompilationConfig("test.jd",
				CompilationConfig.STDOUT, Format.JSON, null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertThat(CompilerFactory.build(cfg, out, DIAGNOSTICS)).isNotNull();
	}

	@Test
	void build_python_format_returns_compiler() {
		CompilationConfig cfg = new CompilationConfig("test.jd",
				CompilationConfig.STDOUT, Format.PYTHON, null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertThat(CompilerFactory.build(cfg, out, DIAGNOSTICS)).isNotNull();
	}

	@Test
	void build_png_format_returns_compiler() {
		CompilationConfig cfg = new CompilationConfig("test.jd",
				CompilationConfig.STDOUT, Format.PNG, null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertThat(CompilerFactory.build(cfg, out, DIAGNOSTICS)).isNotNull();
	}

	@Test
	void build_unsupported_format_throws() {
		CompilationConfig cfg = new CompilationConfig("test.jd",
				CompilationConfig.STDOUT, Format.SVG, null);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		assertThat(CompilerFactory.build(cfg, out, DIAGNOSTICS)).isNotNull();
	}
}
