package ca.mcscert.jpipe.model;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.model.elements.Evidence;
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
	void theFirstSourceKeepsAnIdTwoSourcesShare() {
		// b's ns:a:e and b:ns's a:e are both b:ns:a:e once qualified: a
		// duplicate id, which compilation reports.
		JustificationModel out = ProvenanceFixtures.collision();

		assertThat(out.originOf("b:ns:a:e")).hasValueSatisfying(
				o -> assertThat(o.model().getName()).isEqualTo("b"));
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
