package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Diagnostic;
import ca.mcscert.jpipe.operators.OperatorRegistry;
import ca.mcscert.jpipe.operators.UnificationEquivalenceRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class LoadResolverGlobTest {

	@TempDir
	private Path dir;

	@Test
	void globExpandsIntoEveryMatchingFileInSortedOrder() throws IOException {
		writeTemplate("c.jd", "gamma");
		writeTemplate("a.jd", "alpha");
		writeTemplate("b.jd", "beta");

		CompilationContext ctx = compile("*.jd", null);

		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(templateNames()).containsExactly("alpha", "beta", "gamma");
	}

	@Test
	void singleStarDoesNotCrossDirectoryBoundaries() throws IOException {
		writeTemplate("top.jd", "top");
		writeTemplate("nested/deep.jd", "deep");

		CompilationContext ctx = compile("*.jd", null);

		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(templateNames()).containsExactly("top");
	}

	@Test
	void doubleStarSlashMatchesNestedFilesOnly() throws IOException {
		writeTemplate("top.jd", "top");
		writeTemplate("nested/deep.jd", "deep");

		// Java NIO glob: "**/*.jd" requires a directory segment, so a
		// top-level file does not match.
		CompilationContext ctx = compile("**/*.jd", null);

		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(templateNames()).containsExactly("deep");
	}

	@Test
	void doubleStarMatchesEveryDepthIncludingTopLevel() throws IOException {
		writeTemplate("top.jd", "top");
		writeTemplate("nested/deep.jd", "deep");

		// Java NIO glob: "**.jd" matches any .jd at any depth.
		CompilationContext ctx = compile("**.jd", null);

		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(templateNames()).containsExactly("deep", "top");
	}

	@Test
	void sharedNamespaceIsAppliedToEveryMatchedFile() throws IOException {
		writeTemplate("a.jd", "alpha");
		writeTemplate("b.jd", "beta");

		CompilationContext ctx = compile("*.jd", "lib");

		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(templateNames()).containsExactly("lib:alpha", "lib:beta");
	}

	@Test
	void zeroMatchesIsAFatalError() throws IOException {
		writeTemplate("a.jd", "alpha");

		CompilationContext ctx = compile("none_*.jd", null);

		assertThat(ctx.hasFatalErrors()).isTrue();
		assertThat(fatalMessages(ctx))
				.anyMatch(m -> m.contains("No file matches load pattern"));
	}

	@Test
	void invalidGlobSyntaxIsAFatalErrorAndDoesNotThrow() throws IOException {
		writeTemplate("a.jd", "alpha");

		// "[.jd" is an unbalanced glob; it must not crash compilation.
		CompilationContext ctx = compile("[.jd", null);

		assertThat(ctx.hasFatalErrors()).isTrue();
		assertThat(fatalMessages(ctx))
				.anyMatch(m -> m.contains("Invalid glob in load pattern"));
	}

	@Test
	void upwardPatternMatchesFilesOutsideTheSourceDirectory()
			throws IOException {
		writeTemplate("lib/a.jd", "alpha");
		writeTemplate("lib/b.jd", "beta");

		CompilationContext ctx = compileFromSubDirectory("../lib/*.jd", "lib");

		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(templateNames()).containsExactly("lib:alpha", "lib:beta");
	}

	@Test
	void upwardRecursivePatternMatchesEveryDepth() throws IOException {
		writeTemplate("lib/a.jd", "alpha");
		writeTemplate("lib/nested/deep.jd", "deep");

		CompilationContext ctx = compileFromSubDirectory("../lib/**.jd", null);

		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(templateNames()).containsExactly("alpha", "deep");
	}

	@Test
	void absolutePatternIsAnchoredAtItsOwnRoot() throws IOException {
		writeTemplate("lib/a.jd", "alpha");
		writeTemplate("elsewhere/b.jd", "beta");

		CompilationContext ctx = compileFromSubDirectory(
				dir.resolve("lib") + "/*.jd", null);

		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(templateNames()).containsExactly("alpha");
	}

	@Test
	void anchoredPatternDoesNotWalkSiblingDirectories() throws IOException {
		writeTemplate("models/a.jd", "alpha");
		writeTemplate("other/b.jd", "beta");

		CompilationContext ctx = compile("models/*.jd", null);

		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(templateNames()).containsExactly("alpha");
	}

	@Test
	void upwardSegmentAfterAWildcardIsAFatalError() throws IOException {
		writeTemplate("a.jd", "alpha");

		// A directory walk only ever descends, so this can never match.
		CompilationContext ctx = compile("*/../a.jd", null);

		assertThat(ctx.hasFatalErrors()).isTrue();
		assertThat(fatalMessages(ctx)).anyMatch(m -> m
				.contains("'..' may only appear before the first wildcard"));
	}

	@Test
	void patternAnchoredAtAMissingDirectoryIsAFatalError() throws IOException {
		writeTemplate("lib/a.jd", "alpha");

		CompilationContext ctx = compileFromSubDirectory("../nope/*.jd", null);

		assertThat(ctx.hasFatalErrors()).isTrue();
		assertThat(fatalMessages(ctx))
				.anyMatch(m -> m.contains("is not a directory"));
	}

	@Test
	@DisabledOnOs(OS.WINDOWS)
	void anIoFailureWhileWalkingIsAFatalErrorNotANoMatch() throws IOException {
		writeTemplate("lib/a.jd", "alpha");
		Path lib = dir.resolve("lib");
		assumeTrue(lib.toFile().setReadable(false),
				"filesystem does not honour the permission bit");
		// A process running as root reads the directory regardless.
		assumeTrue(!Files.isReadable(lib), "permission bits are not enforced");

		try {
			CompilationContext ctx = compileFromSubDirectory("../lib/*.jd",
					null);

			assertThat(ctx.hasFatalErrors()).isTrue();
			assertThat(fatalMessages(ctx))
					.anyMatch(m -> m.contains("Cannot expand load pattern"));
		} finally {
			lib.toFile().setReadable(true);
		}
	}

	@Test
	void globMatchingTheSourceFileIsReportedAsACycle() throws IOException {
		writeTemplate("root.jd", "root");

		CompilationContext ctx = compile("root.jd", null,
				dir.resolve("root.jd").toString());

		assertThat(ctx.hasFatalErrors()).isTrue();
		assertThat(fatalMessages(ctx))
				.anyMatch(m -> m.contains("Circular load detected"));
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private List<Command> result;

	private CompilationContext compile(String pattern, String namespace)
			throws IOException {
		return compile(pattern, namespace, dir.resolve("main.jd").toString());
	}

	/**
	 * Compiles from a source file one level below the temporary directory, so
	 * that a "../" pattern has somewhere to climb back up to.
	 */
	private CompilationContext compileFromSubDirectory(String pattern,
			String namespace) {
		return compile(pattern, namespace,
				dir.resolve("src/main.jd").toString());
	}

	private CompilationContext compile(String pattern, String namespace,
			String sourcePath) {
		CompilationContext ctx = new CompilationContext(sourcePath);
		LoadResolver resolver = new LoadResolver(new OperatorRegistry(),
				new UnificationEquivalenceRegistry());
		List<Command> input = List
				.of(new LoadResolver.LoadDirective(pattern, namespace));
		this.result = resolver.fire(input, ctx);
		return ctx;
	}

	private List<String> templateNames() {
		return result.stream().filter(CreateTemplate.class::isInstance)
				.map(CreateTemplate.class::cast).map(CreateTemplate::identifier)
				.toList();
	}

	private static List<String> fatalMessages(CompilationContext ctx) {
		return ctx.diagnostics().stream().filter(Diagnostic::isFatal)
				.map(Diagnostic::message).toList();
	}

	private void writeTemplate(String relativePath, String name)
			throws IOException {
		Path file = dir.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file,
				"template " + name + " { conclusion c is \"c\" }\n");
	}
}
