package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.compiler.steps.transformations.LoadResolver.GlobAnchor;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Unit tests for glob anchoring: the split of a pattern into the directory the
 * search starts from and the portion matched relative to it. Anchoring is
 * tested directly rather than through a compilation because some of its cases
 * (an absolute pattern rooted at "/") would otherwise only be observable by
 * walking the whole filesystem.
 */
class LoadResolverAnchorTest {

	private static final Path BASE = Paths.get("/project/src");

	@ParameterizedTest(name = "{0} anchors at {1} with pattern {2}")
	@CsvSource({
			// Descending patterns: the anchor moves down, and the effective
			// pattern loses the prefix, which is what keeps their meaning
			// identical to matching the whole pattern against BASE.
			"models/*.jd,          /project/src/models, *.jd",
			"models/**/*.jd,       /project/src/models, **/*.jd",
			"dir/[ab].jd,          /project/src/dir,    [ab].jd",
			// No literal prefix: the anchor stays at the declaring directory.
			"*.jd,                 /project/src,        *.jd",
			"**.jd,                /project/src,        **.jd",
			"a?.jd,                /project/src,        a?.jd",
			// Upward patterns: the prefix is resolved like any literal path.
			"../library/*.jd,      /project/library,    *.jd",
			"../../shared/**.jd,   /shared,             **.jd",
			// Absolute patterns anchor at their own root, ignoring BASE.
			"/opt/models/*.jd,     /opt/models,         *.jd",
			"/*.jd,                /,                   *.jd",
			// The cut is made before the first wildcard character, so a brace
			// group is never split even when it spans a separator.
			"'{a,b}/*.jd',         /project/src,        '{a,b}/*.jd'",
			"'{foo/bar,baz}/*.jd', /project/src,        '{foo/bar,baz}/*.jd'"})
	void patternIsSplitIntoAnAnchorAndARelativePattern(String pattern,
			String expectedRoot, String expectedPattern) {
		GlobAnchor anchored = LoadResolver.anchor(BASE, pattern);

		assertThat(anchored.root()).isEqualTo(Paths.get(expectedRoot));
		assertThat(anchored.pattern()).isEqualTo(expectedPattern);
	}
}
