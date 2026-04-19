package ca.mcscert.jpipe.visitor;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import org.junit.jupiter.api.Test;

class DotExporterTest {

	@Test
	void export_nodeIdsAreQualifiedWithModelName() {
		String dot = new DotExporter()
				.export(ModelFixtures.simpleJustification());

		assertThat(dot).contains("id=\"j:c\"", "id=\"j:s\"", "id=\"j:e1\"");
	}

	@Test
	void export_shortLabelIsNotWrapped() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "System correct"));

		String dot = new DotExporter().export(j);

		assertThat(dot).contains("label=\"System correct\"");
	}

	@Test
	void export_longLabelIsWrappedAtWordBoundary() {
		Justification j = new Justification("j");
		// 46 chars — exceeds the 40-char wrap width
		j.setConclusion(new Conclusion("c",
				"The system is fully correct and validated"));

		String dot = new DotExporter().export(j);

		assertThat(dot).contains(
				"label=\"The system is fully correct and\\nvalidated\"");
	}

	@Test
	void export_underscoresInLabelAreEscapedForMarkdown() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "s_s_s"));

		String dot = new DotExporter().export(j);

		assertThat(dot).contains("label=\"s\\_s\\_s\"", "id=\"j:c\"");
	}

	@Test
	void export_labelSpecialCharsAreEscapedBeforeWrapping() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "A \"quoted\" label"));

		String dot = new DotExporter().export(j);

		assertThat(dot).contains("label=\"A \\\"quoted\\\" label\"");
	}

	@Test
	void export_templateExpandedElementsCarryModelAndTemplatePrefix() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("t:c", "System correct"));
		j.addElement(new Strategy("t:s", "Testing"));
		j.addElement(new Evidence("t:e1", "Test results"));

		String dot = new DotExporter().export(j);

		assertThat(dot).contains("id=\"j:t:c\"", "id=\"j:t:s\"",
				"id=\"j:t:e1\"");
	}

	@Test
	void export_inheritedElementsAreWrappedInCluster() {
		Template t = new Template("t");
		t.setConclusion(new Conclusion("c", "System correct"));
		t.addElement(new Strategy("s", "Testing"));
		t.addElement(new AbstractSupport("as", "Abstract support"));

		Justification j = new Justification("j");
		j.addElement(new Evidence("e", "Local evidence"));
		j.inline(t, "t");

		String dot = new DotExporter().export(j);

		assertThat(dot).contains("subgraph cluster_t");
	}

	@Test
	void export_localElementsAreNotInCluster() {
		Template t = new Template("t");
		t.setConclusion(new Conclusion("c", "System correct"));
		t.addElement(new Strategy("s", "Testing"));

		Justification j = new Justification("j");
		j.inline(t, "t");
		j.addElement(new Evidence("e", "Local evidence"));

		String dot = new DotExporter().export(j);

		// Local evidence node must appear before any cluster block
		int evidencePos = dot.indexOf("id=\"j:e\"");
		int clusterPos = dot.indexOf("subgraph cluster_t");
		assertThat(evidencePos).isLessThan(clusterPos);
	}

	@Test
	void export_standaloneJustificationWithQualifiedIdHasNoCluster() {
		// A user-defined qualified id must NOT produce a spurious cluster
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "Top"));
		j.addElement(new Evidence("my:id", "User-defined qualified id"));

		String dot = new DotExporter().export(j);

		assertThat(dot).doesNotContain("subgraph cluster_");
	}

	@Test
	void export_overriddenAbstractSupportAppearsInClusterConcreteElementInModel() {
		// Mirrors 005_override.jd: a_template:abs is replaced by a concrete
		// evidence. The abstract support must remain visible in the cluster
		// (with @abstract suffix) while the concrete element is rendered in the
		// model, connected by a dashed inv arrow.
		Template t = new Template("a_template");
		t.setConclusion(new Conclusion("c", "A conclusion"));
		t.addElement(new Strategy("s", "A strategy"));
		t.addElement(new AbstractSupport("abs", "An abstract support"));

		Justification j = new Justification("immediate");
		j.inline(t, "a_template");
		// Simulate OverrideAbstractSupport: remove the inherited placeholder,
		// then add the concrete element (same qualified id, different type)
		j.removeElement("a_template:abs");
		j.addElement(new Evidence("a_template:abs", "An immediate evidence"));

		String dot = new DotExporter().export(j);

		// Abstract support in cluster, suffixed with @abstract
		assertThat(dot).contains("\"immediate:a_template:abs@abstract\"");
		// Concrete override exists in the graph
		assertThat(dot).contains("id=\"immediate:a_template:abs\"");
		// Dashed arrow: abstract ghost → concrete ("fulfilled by")
		assertThat(dot).contains(
				"\"immediate:a_template:abs\" -> \"immediate:a_template:abs@abstract\""
						+ " [arrowhead=empty, style=dotted]");
	}

	@Test
	void export_multiLevelInheritanceProducesNestedClusters() {
		// j implements t2 which implements t1:
		// cluster_t1 must be nested INSIDE cluster_t2
		Template t1 = new Template("t1");
		t1.setConclusion(new Conclusion("c", "Root conclusion"));
		t1.addElement(new Strategy("s1", "Root strategy"));

		Template t2 = new Template("t2");
		t2.addElement(new Strategy("s2", "Mid strategy"));
		t2.inline(t1, "t1");

		Justification j = new Justification("j");
		j.inline(t2, "t2");

		String dot = new DotExporter().export(j);

		assertThat(dot).contains("subgraph cluster_t2", "subgraph cluster_t1",
				"id=\"j:t2:s2\"", "id=\"j:t1:s1\"");
		// Nesting: cluster_t1 must appear after cluster_t2 opens and before
		// cluster_t2 closes — no closing brace between the two headers
		int t2Idx = dot.indexOf("subgraph cluster_t2");
		int t1Idx = dot.indexOf("subgraph cluster_t1");
		assertThat(t1Idx).isGreaterThan(t2Idx);
		String between = dot.substring(t2Idx, t1Idx);
		assertThat(between).doesNotContain("\n}");
	}
}
