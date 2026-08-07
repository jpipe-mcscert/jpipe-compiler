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
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

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

	/**
	 * A pattern that cannot be expanded is a fatal error naming the reason, and
	 * never an exception: an unbalanced "[" is a syntax error, a ".." after a
	 * wildcard can never be satisfied by a downward walk, and a well-formed
	 * pattern that simply hits nothing is a no-match.
	 */
	@ParameterizedTest
	@CsvSource({"'none_*.jd',  No file matches load pattern",
			"'[.jd',       Invalid glob in load pattern",
			"'*/../a.jd',  '''..'' may only appear before the first wildcard'"})
	void anUnexpandablePatternIsAFatalError(String pattern,
			String expectedMessage) throws IOException {
		writeTemplate("a.jd", "alpha");

		CompilationContext ctx = compile(pattern, null);

		assertThat(ctx.hasFatalErrors()).isTrue();
		assertThat(fatalMessages(ctx))
				.anyMatch(m -> m.contains(expectedMessage));
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
	void literalLoadOfTheSourceFileIsReportedAsACycle() throws IOException {
		writeTemplate("root.jd", "root");

		CompilationContext ctx = compile("root.jd", null,
				dir.resolve("root.jd").toString());

		assertThat(ctx.hasFatalErrors()).isTrue();
		assertThat(fatalMessages(ctx))
				.anyMatch(m -> m.contains("Circular load detected"));
	}

	/**
	 * Anchoring puts the search in the declaring file's own directory, so a
	 * pattern that also matches that file's <em>name</em> is a cycle. A
	 * catch-all ("*.jd", "./*.jd", "**.jd") always does; a narrower one only
	 * when the name happens to match, as "ro*.jd" does against root.jd — and
	 * "none_*.jd" does not, which is why
	 * {@link #anUnexpandablePatternIsAFatalError} gets a no-match there rather
	 * than a cycle. Keeping glob-loaded models in a directory of their own
	 * sidesteps the question entirely: "models/*.jd" cannot reach the file that
	 * loads it.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"*.jd", "./*.jd", "**.jd", "ro*.jd"})
	@Timeout(30)
	void globMatchingTheDeclaringFileIsReportedAsACycle(String pattern)
			throws IOException {
		writeTemplate("root.jd", "root");

		CompilationContext ctx = compile(pattern, null,
				dir.resolve("root.jd").toString());

		assertThat(ctx.hasFatalErrors()).isTrue();
		assertThat(fatalMessages(ctx))
				.anyMatch(m -> m.contains("Circular load detected"));
	}

	/**
	 * A relative path is resolved against the directory of the file that
	 * <em>contains</em> the load, not against the file that started the
	 * compilation. Both {@code a/sibling/} and {@code b/sibling/} exist here,
	 * so the two readings give different answers: resolving against the
	 * declaring file (b/mid.jd) yields fromB, resolving against the root file
	 * (a/main.jd) would yield fromA. Covers the literal and the glob branch
	 * alike.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"sibling/*.jd", "./sibling/*.jd",
			"sibling/fromB.jd"})
	void nestedRelativePathIsResolvedAgainstTheFileThatDeclaresIt(
			String nestedPattern) throws IOException {
		writeTemplate("a/sibling/fromA.jd", "fromA");
		writeTemplate("b/sibling/fromB.jd", "fromB");
		writeSource("b/mid.jd", "load \"" + nestedPattern + "\" as s\n");

		CompilationContext ctx = compile("../b/mid.jd", "m",
				dir.resolve("a/main.jd").toString());

		assertThat(ctx.hasFatalErrors()).isFalse();
		assertThat(templateNames()).containsExactly("m:s:fromB");
	}

	/**
	 * Two directories globbing into each other: a/a.jd pulls in every model of
	 * b/, and b/b.jd pulls in every model of a/ — which includes a.jd itself.
	 * The load stack catches it, so the expansion terminates instead of
	 * recursing forever.
	 */
	@Test
	@Timeout(30)
	void mutualGlobLoadsAcrossDirectoriesTerminateWithACycleError()
			throws IOException {
		writeTemplate("a/a.jd", "ta");
		writeSource("b/b.jd", """
				load "../a/*.jd" as aa
				template tb { conclusion c is "c" }
				""");

		CompilationContext ctx = compile("../b/*.jd", "bb",
				dir.resolve("a/a.jd").toString());

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
		writeSource(relativePath,
				"template " + name + " { conclusion c is \"c\" }\n");
	}

	private void writeSource(String relativePath, String content)
			throws IOException {
		Path file = dir.resolve(relativePath);
		Files.createDirectories(file.getParent());
		Files.writeString(file, content);
	}
}
