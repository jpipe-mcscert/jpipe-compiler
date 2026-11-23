package ca.mcscert.jpipe.model;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.visitor.DotExporter;
import ca.mcscert.jpipe.visitor.JpipeExporter;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Where the copies a model holds come from, recorded when made (#209). */
class OriginTest {

	@Test
	void templateExpansionRecordsWhereEachCopyComesFrom() {
		JustificationModel leaf = ProvenanceFixtures.templateChain();
		JustificationModel mid = leaf.getParent().orElseThrow();

		assertThat(leaf.originOf("base:mid:m")).contains(new Origin(mid, "m"));
		// Already qualified, so kept as is: still a copy of mid's.
		assertThat(leaf.originOf("root:s")).contains(new Origin(mid, "root:s"));
		assertThat(leaf.originOf("root:c")).contains(new Origin(mid, "root:c"));
	}

	@Test
	void anOwnElementHasNoOrigin() {
		Justification j = new Justification("j");
		j.addElement(new Evidence("e", "Own"));

		assertThat(j.originOf("e")).isEmpty();
	}

	@Test
	void aCompositionRecordsEachElementAndAliasOfItsSources() {
		Map<String, JustificationModel> models = ProvenanceFixtures
				.nestedNamespaces();
		JustificationModel b = models.get("b");
		JustificationModel out = models.get("out");
		b.recordAlias("k:old", "k:e");
		Justification again = new Justification("again");
		again.recordComposition(
				new Composition("assemble", List.of(b), Map.of(), Map.of()));

		assertThat(out.originOf("b:ns:a:e")).contains(new Origin(b, "ns:a:e"));
		assertThat(again.originOf("b:k:old")).hasValueSatisfying(o -> {
			assertThat(o).isEqualTo(new Origin(b, "k:old"));
			assertThat(o.element()).isEmpty();
		});
	}

	@Test
	void anIdTwoSourcesShareKeepsBothOriginsTheFirstFirst() {
		// b's ns:a:e and b:ns's a:e are both b:ns:a:e once qualified: a
		// duplicate id, which compilation reports.
		JustificationModel out = ProvenanceFixtures.collision();

		assertThat(out.originOf("b:ns:a:e")).hasValueSatisfying(
				o -> assertThat(o.model().getName()).isEqualTo("b"));
		assertThat(out.origins().get("b:ns:a:e"))
				.extracting(o -> o.model().getName())
				.containsExactly("b", "b:ns");
	}

	@Test
	void theQueriesStillPartitionTheElementsWhenTheTemplateChangesAfterwards() {
		Template t = template("t");
		Justification j = new Justification("j");
		j.inline(t, "t");
		t.removeElement("e");

		assertThat(j.ownElements()).extracting("id").containsExactly("t:e");
		assertThat(j.declaredElements()).extracting("id")
				.containsExactly("t:e");
		assertThat(j.inheritedElements()).extracting("id")
				.containsExactly("t:s");
	}

	@Test
	void aModelWithItsOwnConclusionBeforeExpansionIsStillExported() {
		// Only a caller of the API builds it: the compiler reports an own
		// conclusion under implements as inherited-conclusion.
		Template t = template("t");
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("own", "Own"));
		j.inline(t, "t");

		assertThat(j.copyOf(t, "c")).isEmpty();
		assertThat(new JpipeExporter().export(j)).contains("implements t");
		assertThat(new DotExporter().export(j)).contains("digraph");
	}

	/** {@code c <- s <- e}, with {@code e} an evidence. */
	private static Template template(String name) {
		Template t = new Template(name);
		Conclusion c = new Conclusion("c", "C");
		Strategy s = new Strategy("s", "S");
		Evidence e = new Evidence("e", "E");
		c.addSupport(s);
		s.addSupport(e);
		t.setConclusion(c);
		t.addElement(s);
		t.addElement(e);
		return t;
	}

	@Test
	void aCopyIsFoundThroughTheChainOfTemplates() {
		JustificationModel leaf = ProvenanceFixtures.templateChain();
		JustificationModel mid = leaf.getParent().orElseThrow();
		JustificationModel root = mid.getParent().orElseThrow();

		assertThat(leaf.copyOf(root, "abs")).contains("root:abs");
		assertThat(leaf.copyOf(mid, "m")).contains("base:mid:m");
		assertThat(leaf.copyOf(leaf, "x")).contains("x");
		assertThat(leaf.copyOf(root, "nowhere")).isEmpty();
	}

	@Test
	void anOverrideKeepsTheOriginOfThePlaceholderItReplaces() {
		JustificationModel leaf = ProvenanceFixtures.templateChain();
		JustificationModel mid = leaf.getParent().orElseThrow();

		assertThat(mid.concreteOverrides()).extracting("id")
				.containsExactly("root:abs");
		assertThat(mid.originOf("root:abs"))
				.hasValueSatisfying(o -> assertThat(o.element()).get()
						.extracting("id").isEqualTo("abs"));
	}

	@Test
	void anExactIdIsFoundWithoutTheShortNameFallback() {
		Justification j = new Justification("j");
		Evidence inherited = new Evidence("t:e", "Inherited");
		j.addElement(inherited);

		assertThat(j.elementWithId("t:e")).contains(inherited);
		assertThat(j.elementWithId("e")).isEmpty();
		assertThat(j.findById("e")).contains(inherited);
	}
}
