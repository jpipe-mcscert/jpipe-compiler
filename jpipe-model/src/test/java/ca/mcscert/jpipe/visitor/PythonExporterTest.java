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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PythonExporterTest {

	@ParameterizedTest
	@ValueSource(strings = {"def testing(", "def test_results(",
			"def testing() -> Outcome:", "def test_results() -> Outcome:"})
	void export_simpleJustificationMethodSignaturesArePresent(String expected) {
		assertThat(new PythonExporter()
				.export(ModelFixtures.simpleJustification()))
				.contains(expected);
	}

	@Test
	void export_evidenceAndStrategyAreActive() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).contains("\n@strategy(\"j:s\"", "\n@evidence(\"j:e1\"")
				.doesNotContain("# @strategy(", "# @evidence(");
	}

	@Test
	void export_conclusion_isCommentedOutWithWhyAndHowToEnableIt() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).contains(
				"""
						# The runner concludes this claim from what supports it unless it is bound. Uncomment to check it.
						# @conclusion("j:c", consumes=[])
						# def the_system_is_correct() -> Outcome:
						#     \"""[conclusion] The system is correct\"""
						#     raise NotImplementedError("j:c is not implemented yet")
						""");
	}

	@Test
	void export_conclusion_comesLastInItsNamespace() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).containsSubsequence("def test_results(", "def testing(",
				"def the_system_is_correct(");
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

		assertThat(py).contains(
				"""
						# The runner passes this claim through unless it is bound. Uncomment to check it.
						# @sub_conclusion("j:sc", consumes=[], produces=[])
						""");
	}

	@Test
	void export_mergedElement_isNamedByItsOriginalsNotByTheMergeId() {
		Justification j = ModelFixtures.simpleJustification();
		j.recordAlias("a:s1", "s");
		j.recordAlias("b:s2", "s");

		String py = new PythonExporter().export(j);

		assertThat(py).contains("@strategy(\"a:s1\", \"b:s2\", consumes=[]")
				.doesNotContain("\"j:s\"");
	}

	@Test
	void export_originalsSharingAPlainId_staySeparatedByTheirSourceModel() {
		String py = new PythonExporter()
				.export(ModelFixtures.unifiedJustification());

		assertThat(py).contains("@strategy(\"a:s\", \"b:s\", consumes=[]")
				.doesNotContain("\"s\"");
	}

	@Test
	void export_originalsShorteningToTheSameForm_areEmittedOnce() {
		Justification j = ModelFixtures.simpleJustification();
		j.recordAlias("a:s", "s");
		j.recordAlias("outer:a:s", "s"); // also shortens to "a:s"

		String py = new PythonExporter().export(j);

		assertThat(py).contains("@strategy(\"a:s\", consumes=[]")
				.doesNotContain("\"a:s\", \"a:s\"");
	}

	@Test
	void export_chainedAliases_isNamedByTheOriginalSourceModelIds() {
		String py = new PythonExporter()
				.export(ModelFixtures.chainedAliasJustification());

		assertThat(py).contains("@strategy(\"a:s1\", \"b:s2\", consumes=[]")
				.doesNotContain("\"j:s\"", "\"outer:s\"");
	}

	@Test
	void export_unifiedIdBesideItsOriginals_isStillLeftOut() {
		String py = new PythonExporter()
				.export(ModelFixtures.siblingUnifiedIdJustification());

		assertThat(py).contains("@strategy(\"a:s1\", \"b:s2\", consumes=[]")
				.doesNotContain("unified_0");
	}

	@Test
	void export_cyclicAliases_fallsBackToTheElementsOwnId() {
		Justification j = ModelFixtures.simpleJustification();
		j.recordAlias("s", "x");
		j.recordAlias("x", "s");

		String py = new PythonExporter().export(j);

		assertThat(py).contains("@strategy(\"j:s\", consumes=[]");
	}

	@Test
	void export_ambiguousShortId_keepsEnoughQualificationToDesignateOne() {
		String py = new PythonExporter()
				.export(ModelFixtures.ambiguousShortIdJustification());

		assertThat(py).contains("(\"a:e\", ", "(\"b:e\", ")
				.doesNotContain("\"e\"", "\"j:e\"");
	}

	@Test
	void export_everyBoundIdInTheModuleIsDistinct() {
		String py = new PythonExporter()
				.export(ModelFixtures.chainedAliasJustification());

		List<String> links = Pattern
				.compile("^(?:# )?@\\w+\\((.*)\\)$", Pattern.MULTILINE)
				.matcher(py).results()
				.flatMap(m -> Pattern.compile("\"([^\"]+)\"")
						.matcher(m.group(1)).results())
				.map(m -> m.group(1)).toList();

		assertThat(links).isNotEmpty().doesNotHaveDuplicates();
	}

	@Test
	void export_identifierDesignatingTwoElements_isRefusedNotEmitted() {
		Justification j = ModelFixtures.simpleJustification();
		// "e1" is the evidence's own id and, through this alias, also names the
		// strategy, so both functions would be bound by "j:e1".
		j.recordAlias("e1", "s");
		PythonExporter exporter = new PythonExporter();

		assertThatThrownBy(() -> exporter.export(j))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContainingAll("\"j:e1\"", "testing", "test_results",
						"unique-identifiers");
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
				"def t_strategy(");
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
	void export_decoratorDeclarationsReflectElementKind() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).contains(
				"@strategy(\"j:s\", consumes=[], produces=[])\n"
						+ "def testing() -> Outcome:",
				"@evidence(\"j:e1\", observes={}, produces=[])\n"
						+ "def test_results() -> Outcome:");
	}

	@Test
	void export_everyStepRaisesUntilItIsWritten() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py).contains(
				"raise NotImplementedError(\"j:s is not implemented"
						+ " yet\")",
				"raise NotImplementedError(\"j:e1 is not implemented"
						+ " yet\")")
				.doesNotContain("    pass\n");
	}

	@Test
	void export_preambleImportsFromThePackageRootTheDecoratorsUsed() {
		String py = new PythonExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(py)
				.startsWith(
						"""
								from jpipe_runner import Fail, Outcome, Pass, conclusion, evidence, strategy

								""")
				.doesNotContain("sub_conclusion", "jpipe_runner.framework",
						"JpipeProduce", "typing");
	}

	@Test
	void export_preambleImportsTheDecoratorsOfCommentedOutSteps() {
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
				.startsWith("from jpipe_runner import Fail, Outcome, Pass,"
						+ " conclusion, strategy, sub_conclusion\n");
	}

	@Test
	void export_abstractSupportIsWrittenAsEvidence() {
		String py = new PythonExporter().export(ModelFixtures.simpleTemplate());

		assertThat(py).containsPattern(
				"@evidence\\(\"[^\"]+\", observes=\\{}, produces=\\[]\\)\n"
						+ "def abstract_step\\(\\) -> Outcome:");
	}

	@Test
	void export_withSourcePath_includesHeader() {
		String py = new PythonExporter().export(
				ModelFixtures.simpleJustification(), "examples/simple.jd");

		assertThat(py).contains("# Generated by jPipe",
				"# Source : examples/simple.jd", "# Date   :",
				"# Step library for jpipe-runner 4: "
						+ PythonExporter.AUTHORING_GUIDE + "\n\n"
						+ "from jpipe_runner import ");
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
				.doesNotContain("t:abs is not implemented yet");
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

		assertThat(py).contains("def accuracy_0_85() -> Outcome:",
				"\"\"\"[evidence] Accuracy <= 0.85\"\"\"");
	}

	@Test
	void export_sameLabelInTwoNamespaces_isPrefixedWithEachNamespace() {
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "Overall");
		Strategy a = new Strategy("a:s", "A strategy");
		Strategy b = new Strategy("b:s", "A strategy");
		Evidence e = new Evidence("e", "Proof");
		j.setConclusion(c);
		j.addElement(a);
		j.addElement(b);
		j.addElement(e);
		c.addSupport(a);

		String py = new PythonExporter().export(j);

		assertThat(py).contains("def a_a_strategy(", "def b_a_strategy(",
				"def proof(").doesNotContain("def a_strategy(");
	}

	@Test
	void export_sameLabelInOneNamespace_isPrefixedWithEachBoundId() {
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "Overall");
		Strategy s1 = new Strategy("s1", "A strategy");
		Strategy s2 = new Strategy("s2", "A strategy");
		j.setConclusion(c);
		j.addElement(s1);
		j.addElement(s2);
		c.addSupport(s1);

		String py = new PythonExporter().export(j);

		assertThat(py).contains("def j_s1_a_strategy(", "def j_s2_a_strategy(")
				.doesNotContain("def a_strategy(", "def j_a_strategy(");
	}

	@Test
	void export_namespacedNameTakenByAnotherFunction_fallsBackToTheBoundId() {
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "Overall");
		Strategy a = new Strategy("a:s", "Check");
		Strategy b = new Strategy("b:s", "Check");
		Evidence taken = new Evidence("e", "A check");
		j.setConclusion(c);
		j.addElement(a);
		j.addElement(b);
		j.addElement(taken);
		a.addSupport(taken);
		c.addSupport(a);

		String py = new PythonExporter().export(j);

		assertThat(py).contains("def a_check(", "def a_s_check(",
				"def b_check(");
	}

	@Test
	void export_sameLabelOnACommentedOutStep_isPrefixedToo() {
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "Holds");
		Strategy s = new Strategy("x:s", "Holds");
		j.setConclusion(c);
		j.addElement(s);
		c.addSupport(s);

		String py = new PythonExporter().export(j);

		assertThat(py).contains("\ndef x_holds(", "# def j_holds(");
	}

	@ParameterizedTest
	@ValueSource(strings = {"Evidence", "Strategy", "Sub conclusion",
			"Conclusion", "Pass", "If", "Import"})
	void export_labelEscapingToAReservedName_isPrefixedWithItsNamespace(
			String label) {
		Justification j = new Justification("j");
		Conclusion c = new Conclusion("c", "Overall");
		Strategy s = new Strategy("s", label);
		j.setConclusion(c);
		j.addElement(s);
		c.addSupport(s);
		String reserved = label.toLowerCase().replace(' ', '_');

		String py = new PythonExporter().export(j);

		assertThat(py).contains("def j_" + reserved + "() -> Outcome:")
				.doesNotContain("def " + reserved + "(");
	}

	@ParameterizedTest
	@CsvSource({"First proof, Second proof", "Proof, Proof"})
	void export_twoElementsSharingAnId_isRefusedNotMergedIntoOneFunction(
			String firstLabel, String secondLabel) {
		Justification j = ModelFixtures.simpleJustification();
		// Consistency reports the duplicate id, but does not stop the export.
		j.addElement(new Evidence("dup", firstLabel));
		j.addElement(new Evidence("dup", secondLabel));
		PythonExporter exporter = new PythonExporter();

		assertThatThrownBy(() -> exporter.export(j))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContainingAll("\"j:dup\"", "no-duplicate-ids");
	}
}
