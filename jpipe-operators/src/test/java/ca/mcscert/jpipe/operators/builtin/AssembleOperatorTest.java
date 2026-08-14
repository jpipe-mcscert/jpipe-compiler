package ca.mcscert.jpipe.operators.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.operators.IncompatibleUnificationException;
import ca.mcscert.jpipe.operators.InvalidOperatorCallException;
import ca.mcscert.jpipe.operators.ModelKind;
import ca.mcscert.jpipe.operators.UnificationEquivalenceRegistry;
import ca.mcscert.jpipe.operators.Unifier;
import ca.mcscert.jpipe.operators.equivalences.SameLabel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssembleOperatorTest {

	private ExecutionEngine engine;
	private AssembleOperator assemble;
	private Unifier unifier;

	@BeforeEach
	void setUp() {
		engine = new ExecutionEngine();
		assemble = new AssembleOperator();
		UnificationEquivalenceRegistry registry = new UnificationEquivalenceRegistry();
		registry.register("sameLabel", new SameLabel());
		unifier = new Unifier(registry);
	}

	private static final Map<String, String> ARGS = Map.of("conclusionLabel",
			"A global conclusion", "strategyLabel", "An aggregating strategy");

	// ── fixtures ─────────────────────────────────────────────────────────────

	private Justification buildJustification(String name, String cLabel,
			String sLabel, String eLabel) {
		List<Command> cmds = new ArrayList<>();
		cmds.add(new CreateJustification(name));
		cmds.add(new CreateConclusion(name, "c", cLabel));
		cmds.add(new CreateStrategy(name, "s", sLabel));
		cmds.add(new CreateEvidence(name, "e", eLabel));
		cmds.add(new AddSupport(name, "c", "s"));
		cmds.add(new AddSupport(name, "s", "e"));
		Unit unit = engine.spawn("src", cmds);
		return (Justification) unit.get(name);
	}

	private Template buildTemplate(String name) {
		List<Command> cmds = new ArrayList<>();
		cmds.add(new CreateTemplate(name));
		cmds.add(new CreateConclusion(name, "c", "A conclusion"));
		cmds.add(new CreateStrategy(name, "s", "A strategy"));
		cmds.add(new AddSupport(name, "c", "s"));
		Unit unit = engine.spawn("src", cmds);
		return (Template) unit.get(name);
	}

	private Justification twoSourceResult() {
		var a = buildJustification("a_claim", "A conclusion", "A strategy",
				"An evidence");
		var b = buildJustification("another_claim", "Another conclusion",
				"Another strategy", "Another evidence");
		List<Command> cmds = assemble.apply("assembled", List.of(a, b), ARGS);
		Unit unit = engine.spawn("out", cmds);
		return (Justification) unit.get("assembled");
	}

	// ── ResultType ───────────────────────────────────────────────────────────

	@Nested
	class ResultType {

		@Test
		void twoJustificationsProduceJustification() {
			var a = buildJustification("a", "C", "S", "E");
			var b = buildJustification("b", "C", "S", "E");
			List<Command> cmds = assemble.apply("result", List.of(a, b), ARGS);
			Unit unit = engine.spawn("out", cmds);
			assertThat(unit.get("result")).isInstanceOf(Justification.class);
		}

		@Test
		void anyTemplateSourceProducesTemplate() {
			var j = buildJustification("j", "C", "S", "E");
			var t = buildTemplate("t");
			List<Command> cmds = assemble.apply("result", List.of(j, t), ARGS);
			Unit unit = engine.spawn("out", cmds);
			assertThat(unit.get("result")).isInstanceOf(Template.class);
		}

		@Test
		void resultKindIsJustificationWhenAllSourcesAreJustifications() {
			var a = buildJustification("a", "C", "S", "E");
			var b = buildJustification("b", "C", "S", "E");
			assertThat(assemble.resultKind(List.of(a, b), ARGS))
					.isEqualTo(ModelKind.JUSTIFICATION);
		}

		@Test
		void resultKindIsTemplateWhenAnySourceIsTemplate() {
			var j = buildJustification("j", "C", "S", "E");
			var t = buildTemplate("t");
			assertThat(assemble.resultKind(List.of(j, t), ARGS))
					.isEqualTo(ModelKind.TEMPLATE);
		}
	}

	// ── ResultStructure ──────────────────────────────────────────────────────

	@Nested
	class ResultStructure {

		@Test
		void conclusionsAreDemotedToSubConclusions() {
			Justification result = twoSourceResult();
			// The synthesized conclusion is the only Conclusion in the result
			assertThat(result.conclusion()).isPresent();
			assertThat(result.conclusion().get().id())
					.isEqualTo(AssembleOperator.CONCLUSION_ID);
			// Source conclusions have been demoted to SubConclusions
			assertThat(result.subConclusions()).hasSize(2);
		}

		@Test
		void demotedSubConclusionsHaveSourcePrefixedIds() {
			Justification result = twoSourceResult();
			assertThat(result.subConclusions()).extracting(SubConclusion::id)
					.containsExactlyInAnyOrder("a_claim:c", "another_claim:c");
		}

		@Test
		void strategiesHaveSourcePrefixedIds() {
			Justification result = twoSourceResult();
			assertThat(result.strategies()).extracting(Strategy::id)
					.contains("a_claim:s", "another_claim:s");
		}

		@Test
		void synthesizedStrategyAndConclusionAreCreated() {
			Justification result = twoSourceResult();
			assertThat(result.strategies()).extracting(Strategy::id)
					.contains(AssembleOperator.STRATEGY_ID);
			assertThat(result.conclusion()).isPresent();
			assertThat(result.conclusion().get().id())
					.isEqualTo(AssembleOperator.CONCLUSION_ID);
		}

		@Test
		void synthesizedStrategyHasCorrectLabel() {
			Justification result = twoSourceResult();
			Strategy s = result.strategies().stream()
					.filter(st -> st.id().equals(AssembleOperator.STRATEGY_ID))
					.findFirst().orElseThrow();
			assertThat(s.label()).isEqualTo("An aggregating strategy");
		}

		@Test
		void synthesizedConclusionHasCorrectLabel() {
			Justification result = twoSourceResult();
			assertThat(result.conclusion().get().label())
					.isEqualTo("A global conclusion");
		}
	}

	// ── Edges ────────────────────────────────────────────────────────────────

	@Nested
	class Edges {

		@Test
		void sourceEdgesArePreserved() {
			Justification result = twoSourceResult();

			// a_claim:s supports a_claim:c (demoted)
			SubConclusion aClaimC = result.subConclusions().stream()
					.filter(sc -> sc.id().equals("a_claim:c")).findFirst()
					.orElseThrow();
			assertThat(aClaimC.getSupport()).isPresent();
			assertThat(aClaimC.getSupport().get().id()).isEqualTo("a_claim:s");

			// a_claim:e supports a_claim:s
			Strategy aClaimS = result.strategies().stream()
					.filter(s -> s.id().equals("a_claim:s")).findFirst()
					.orElseThrow();
			assertThat(aClaimS.getSupports()).hasSize(1);
			assertThat(aClaimS.getSupports().get(0).toString())
					.contains("a_claim:e");
		}

		@Test
		void demotedSubConclusionsSupportNewStrategy() {
			Justification result = twoSourceResult();
			Strategy assembleS = result.strategies().stream()
					.filter(s -> s.id().equals(AssembleOperator.STRATEGY_ID))
					.findFirst().orElseThrow();
			assertThat(assembleS.getSupports()).hasSize(2);
			assertThat(assembleS.getSupports())
					.extracting(leaf -> ((SubConclusion) leaf).id())
					.containsExactlyInAnyOrder("a_claim:c", "another_claim:c");
		}

		@Test
		void newStrategySupportsNewConclusion() {
			Justification result = twoSourceResult();
			assertThat(result.conclusion()).isPresent();
			assertThat(result.conclusion().flatMap(Conclusion::getSupport))
					.isPresent().get().extracting(Strategy::id)
					.isEqualTo(AssembleOperator.STRATEGY_ID);
		}
	}

	// ── NaryCase ─────────────────────────────────────────────────────────────

	@Nested
	class NaryCase {

		@Test
		void threeSourcesProducesThreeDemotedSubConclusions() {
			var a = buildJustification("a_claim", "A conclusion", "A strategy",
					"An evidence");
			var b = buildJustification("another_claim", "Another conclusion",
					"Another strategy", "Another evidence");
			var c = buildJustification("a_third_claim", "A third conclusion",
					"A third strategy", "A third evidence");
			List<Command> cmds = assemble.apply("assembled_3", List.of(a, b, c),
					ARGS);
			Unit unit = engine.spawn("out", cmds);
			Justification result = (Justification) unit.get("assembled_3");

			assertThat(result.subConclusions()).hasSize(3);
			assertThat(result.subConclusions()).extracting(SubConclusion::id)
					.containsExactlyInAnyOrder("a_claim:c", "another_claim:c",
							"a_third_claim:c");

			Strategy assembleS = result.strategies().stream()
					.filter(s -> s.id().equals(AssembleOperator.STRATEGY_ID))
					.findFirst().orElseThrow();
			assertThat(assembleS.getSupports()).hasSize(3);
		}
	}

	// ── Commutativity ────────────────────────────────────────────────────────

	/**
	 * Assembling the same bricks in either order must give the same model, even
	 * when unification merges a group that mixes element kinds: one source
	 * argues the claim (sub-conclusion), the other still asserts it (evidence).
	 * See issue #156.
	 */
	@Nested
	class Commutativity {

		private static final String SHARED = "A shared claim";

		/** A brick whose shared claim is argued: a sub-conclusion. */
		private Justification arguing(String name) {
			List<Command> cmds = new ArrayList<>();
			cmds.add(new CreateJustification(name));
			cmds.add(new CreateConclusion(name, "c", "Argued holds"));
			cmds.add(new CreateStrategy(name, "s", "because the claim"));
			cmds.add(new CreateSubConclusion(name, "sc", SHARED));
			cmds.add(new CreateStrategy(name, "deeper", "because of details"));
			cmds.add(new CreateEvidence(name, "e", "Some detail"));
			cmds.add(new AddSupport(name, "c", "s"));
			cmds.add(new AddSupport(name, "s", "sc"));
			cmds.add(new AddSupport(name, "sc", "deeper"));
			cmds.add(new AddSupport(name, "deeper", "e"));
			return (Justification) engine.spawn("src", cmds).get(name);
		}

		/** A brick whose shared claim is asserted: a plain evidence. */
		private Justification asserting(String name) {
			return buildJustification(name, "Asserted holds", "also because it",
					SHARED);
		}

		private Justification assembled(String name,
				List<JustificationModel<?>> sources) {
			List<Command> cmds = unifier.unify(name,
					assemble.apply(name, sources, ARGS), ARGS);
			return (Justification) engine.spawn("out", cmds).get(name);
		}

		@Test
		void assemblingThePlainSourceFirstStillBuilds() {
			// The regression: with the evidence met first, the merged element
			// used to be created as an evidence, which the arguing strategy
			// could not support.
			Justification result = assembled("right",
					List.of(asserting("other"), arguing("deep")));

			assertThat(result.subConclusions()).extracting(SubConclusion::id)
					.contains("unified_0");
		}

		@Test
		void bothArgumentOrdersProduceTheSameModel() {
			// Labels are pairwise distinct in these fixtures, so (kind, label)
			// identifies an element and comparing the two sets below is an
			// exact isomorphism check. Ids cannot be compared directly: the
			// unified_N counter follows group-discovery order.
			Justification left = assembled("left",
					List.of(arguing("deep"), asserting("other")));
			Justification right = assembled("right",
					List.of(asserting("other"), arguing("deep")));

			assertThat(nodes(left)).isEqualTo(nodes(right));
			assertThat(edges(left)).isEqualTo(edges(right));
		}

		@Test
		void mergingTheGlobalConclusionWithASourceElementIsRejected() {
			List<JustificationModel<?>> sources = List.of(arguing("deep"),
					asserting("other"));
			Map<String, String> clashing = Map.of("conclusionLabel", SHARED,
					"strategyLabel", "An aggregating strategy");
			List<Command> composed = assemble.apply("clash", sources, clashing);

			assertThatThrownBy(() -> unifier.unify("clash", composed, clashing))
					.isInstanceOf(IncompatibleUnificationException.class)
					.hasMessageContaining("cannot unify")
					.hasMessageContaining(SHARED);
		}

		private Set<String> nodes(JustificationModel<?> model) {
			Set<String> nodes = model.getElements().stream()
					.map(e -> e.kind() + "|" + e.label())
					.collect(Collectors.toCollection(HashSet::new));
			model.conclusion()
					.ifPresent(c -> nodes.add(c.kind() + "|" + c.label()));
			return nodes;
		}

		private Set<String> edges(JustificationModel<?> model) {
			Set<String> edges = new HashSet<>();
			model.strategies().forEach(s -> s.getSupports().forEach(
					leaf -> edges.add(((JustificationElement) leaf).label()
							+ " -> " + s.label())));
			model.subConclusions().forEach(sc -> sc.getSupport().ifPresent(
					s -> edges.add(s.label() + " -> " + sc.label())));
			model.conclusion().flatMap(Conclusion::getSupport)
					.ifPresent(s -> edges.add(s.label() + " -> "
							+ model.conclusion().orElseThrow().label()));
			return edges;
		}
	}

	// ── Validation ───────────────────────────────────────────────────────────

	@Nested
	class Validation {

		@Test
		void throwsWhenConclusionLabelMissing() {
			var a = buildJustification("a", "C", "S", "E");
			List<JustificationModel<?>> sources = List.of(a);
			var args = Map.of("strategyLabel", "s");
			assertThatThrownBy(() -> assemble.apply("r", sources, args))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("conclusionLabel");
		}

		@Test
		void throwsWhenStrategyLabelMissing() {
			var a = buildJustification("a", "C", "S", "E");
			List<JustificationModel<?>> sources = List.of(a);
			var args = Map.of("conclusionLabel", "c");
			assertThatThrownBy(() -> assemble.apply("r", sources, args))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("strategyLabel");
		}
	}
}
