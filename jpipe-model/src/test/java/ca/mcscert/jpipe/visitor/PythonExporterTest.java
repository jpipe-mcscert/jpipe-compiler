package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PythonExporterTest {

	@ParameterizedTest
	@ValueSource(strings = {"def testing(", "def test_results(",
			"def testing(produce: JpipeProduce) -> bool:",
			"def test_results(produce: JpipeProduce) -> bool:"})
	void export_simpleJustificationMethodSignaturesArePresent(String expected) {
		assertThat(new PythonExporter()
				.export(ModelFixtures.simpleJustification()))
				.contains(expected);
	}

	@Test
	void export_jpipeLinkIsActive() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py)
				.contains("\n@jpipe_link(\"j:s\")", "\n@jpipe_link(\"j:e1\")")
				.doesNotContain("\n# @jpipe_link(\"j:s\")",
						"\n# @jpipe_link(\"j:e1\")");
	}

	@Test
	void export_conclusion_isNotGeneratedAtAll() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).doesNotContain("the_system_is_correct", "j:c",
				"[conclusion]");
	}

	@Test
	void export_commentedOutElement_saysWhyAndHowToEnableIt() {
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "Overall");
		Strategy s = new Strategy("s", "By parts");
		SubConclusion sc = new SubConclusion("sc", "Part holds");
		j.setConclusion(c);
		j.addElement(s);
		j.addElement(sc);
		sc.addSupport(s);
		c.addSupport(s);

		String py = new PythonExporter().export(j);

		assertThat(py)
				.contains("Uncomment to check it.\n# @jpipe_link(\"j:sc\")")
				.doesNotContain("j:c");
	}

	@Test
	void export_mergedElement_isNamedByItsOriginalsNotByTheMergeId() {
		Justification j = ModelFixtures.simpleJustification();
		j.recordAlias("a:s1", "s");
		j.recordAlias("b:s2", "s");

		String py = new PythonExporter().export(j);

		assertThat(py)
				.contains(
						"@jpipe_link(\"a:s1\")\n@jpipe_link(\"b:s2\")\n@jpipe(")
				.doesNotContain("@jpipe_link(\"j:s\")");
	}

	@Test
	void export_originalsSharingAPlainId_staySeparatedByTheirSourceModel() {
		String py = new PythonExporter()
				.export(ModelFixtures.unifiedJustification());

		assertThat(py)
				.contains("@jpipe_link(\"a:s\")\n@jpipe_link(\"b:s\")\n@jpipe(")
				.doesNotContain("@jpipe_link(\"s\")");
	}

	@Test
	void export_originalsShorteningToTheSameForm_areEmittedOnce() {
		Justification j = ModelFixtures.simpleJustification();
		j.recordAlias("a:s", "s");
		j.recordAlias("outer:a:s", "s"); // also shortens to "a:s"

		String py = new PythonExporter().export(j);

		assertThat(py).contains("@jpipe_link(\"a:s\")\n@jpipe(")
				.doesNotContain("@jpipe_link(\"a:s\")\n@jpipe_link(\"a:s\")");
	}

	@Test
	void export_chainedAliases_isNamedByTheOriginalSourceModelIds() {
		String py = new PythonExporter()
				.export(ModelFixtures.chainedAliasJustification());

		assertThat(py)
				.contains(
						"@jpipe_link(\"a:s1\")\n@jpipe_link(\"b:s2\")\n@jpipe(")
				.doesNotContain("@jpipe_link(\"j:s\")",
						"@jpipe_link(\"outer:s\")");
	}

	@Test
	void export_unifiedIdBesideItsOriginals_isStillLeftOut() {
		String py = new PythonExporter()
				.export(ModelFixtures.siblingUnifiedIdJustification());

		assertThat(py)
				.contains(
						"@jpipe_link(\"a:s1\")\n@jpipe_link(\"b:s2\")\n@jpipe(")
				.doesNotContain("unified_0");
	}

	@Test
	void export_cyclicAliases_fallsBackToTheElementsOwnId() {
		Justification j = ModelFixtures.simpleJustification();
		j.recordAlias("s", "x");
		j.recordAlias("x", "s");

		String py = new PythonExporter().export(j);

		assertThat(py).contains("@jpipe_link(\"j:s\")\n@jpipe(");
	}

	@Test
	void export_ambiguousShortId_keepsEnoughQualificationToDesignateOne() {
		String py = new PythonExporter()
				.export(ModelFixtures.ambiguousShortIdJustification());

		assertThat(py).contains("@jpipe_link(\"a:e\")", "@jpipe_link(\"b:e\")")
				.doesNotContain("@jpipe_link(\"e\")", "@jpipe_link(\"j:e\")");
	}

	@Test
	void export_everyJpipeLinkInTheModuleIsDistinct() {
		String py = new PythonExporter()
				.export(ModelFixtures.chainedAliasJustification());

		List<String> links = Pattern.compile("@jpipe_link\\(\"(.+)\"\\)")
				.matcher(py).results().map(m -> m.group(1)).toList();

		assertThat(links).isNotEmpty().doesNotHaveDuplicates();
	}

	@Test
	void export_identifierDesignatingTwoElements_isRefusedNotEmitted() {
		Justification j = ModelFixtures.simpleJustification();
		// "e1" is the evidence's own id and, through this alias, also names the
		// strategy, so both functions would claim @jpipe_link("e1").
		j.recordAlias("e1", "s");
		PythonExporter exporter = new PythonExporter();

		assertThatThrownBy(() -> exporter.export(j))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContainingAll("@jpipe_link(\"j:e1\")", "testing",
						"test_results", "unique-identifiers");
	}

	@Test
	void export_opensEachNamespaceWithABannerSizedToItsName() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).contains("### ###\n## j ##\n### ###\n");
	}

	@Test
	void export_groupsElementsByNamespaceAlphabetically() {
		String py = new PythonExporter()
				.export(ModelFixtures.ambiguousShortIdJustification());

		assertThat(py).containsSubsequence("## a ##", "def first(", "## b ##",
				"def second(", "## j ##");
	}

	@Test
	void export_ordersElementsBottomUpWithinANamespace() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).containsSubsequence("def test_results(", "def testing(");
	}

	@Test
	void export_ordersSubConclusionsBetweenStrategiesAndTheConclusion() {
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "Overall");
		Strategy s = new Strategy("s", "By parts");
		SubConclusion sc = new SubConclusion("sc", "Part holds");
		Evidence e = new Evidence("e", "Proof");
		j.setConclusion(c);
		j.addElement(s);
		j.addElement(sc);
		j.addElement(e);
		s.addSupport(e);
		sc.addSupport(s);
		c.addSupport(s);

		String py = new PythonExporter().export(j);

		assertThat(py).containsSubsequence("def proof(", "def by_parts(",
				"def part_holds(");
	}

	@Test
	void export_ordersAbstractSupportsWithTheEvidence() {
		String py = new PythonExporter().export(ModelFixtures.simpleTemplate());

		assertThat(py).containsSubsequence("def abstract_step(",
				"def strategy(");
	}

	@Test
	void export_mergedElement_isGroupedWithTheComposedModelNotASource() {
		Justification j = new Justification("comp");
		Conclusion c = new Conclusion("c", "Overall");
		Strategy s = new Strategy("src:s", "By parts");
		Evidence merged = new Evidence("merged", "Shared proof");
		j.setConclusion(c);
		j.addElement(s);
		j.addElement(merged);
		s.addSupport(merged);
		c.addSupport(s);
		j.recordAlias("src:e1", "merged");
		j.recordAlias("other:e2", "merged");

		String py = new PythonExporter().export(j);

		assertThat(py).containsSubsequence("## comp ##", "def shared_proof(",
				"## src ##");
	}

	@Test
	void export_decoratorArgsReflectElementRole() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).contains(
				"@jpipe_link(\"j:s\")\n@jpipe(produce=[], consume=[])",
				"@jpipe_link(\"j:e1\")\n@jpipe(produce=[])");
	}

	@Test
	void export_preambleContainsImportsAndTypeAlias() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).contains("from typing import Any, Callable",
				"from jpipe_runner.framework.decorators"
						+ ".jpipe_decorator import jpipe",
				"from jpipe_runner.framework.decorators"
						+ ".link_decorator import jpipe_link",
				"JpipeProduce = Callable[[str, Any], None]");
	}

	@Test
	void export_withSourcePath_includesHeader() {
		String py = new PythonExporter().export(
				ModelFixtures.simpleJustification(), "examples/simple.jd");

		assertThat(py).contains("# Generated by jPipe",
				"# Source : examples/simple.jd", "# Date   :");
	}

	@Test
	void export_withoutSourcePath_noHeader() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).doesNotContain("# Generated by jPipe");
	}

	@Test
	void export_abstractSupportRaisesNotImplementedError() {
		Template t = new Template("t");
		Conclusion c = new Conclusion("c", "Conclusion");
		Strategy s = new Strategy("s", "Strategy");
		AbstractSupport abs = new AbstractSupport("abs", "Abstract step");
		t.setConclusion(c);
		t.addElement(s);
		t.addElement(abs);
		s.addSupport(abs);
		c.addSupport(s);

		String py = new PythonExporter().export(t);

		assertThat(py)
				.contains("raise NotImplementedError(\"t:abs is abstract"
						+ " and must be overridden before execution\")")
				.doesNotContain("def abstract_step():\n    pass");
	}

	@Test
	void export_docstringIncludesTypeAndLabel() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).contains("\"\"\"[strategy] Testing\"\"\"",
				"\"\"\"[evidence] Test results\"\"\"");
	}

	@Test
	void export_labelWithSpecialChars_methodNameEscapedDocstringPreserved() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "Overall"));
		j.addElement(new Evidence("e", "Accuracy <= 0.85"));

		String py = new PythonExporter().export(j);

		assertThat(py).contains("def accuracy_0_85(produce: JpipeProduce)",
				"\"\"\"[evidence] Accuracy <= 0.85\"\"\"");
	}
}
