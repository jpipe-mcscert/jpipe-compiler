package ca.mcscert.jpipe.operators.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.operators.InvalidOperatorCallException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class AssembleOperatorTest {

	private ExecutionEngine engine;
	private AssembleOperator assemble;

	@BeforeEach
	void setUp() {
		engine = new ExecutionEngine();
		assemble = new AssembleOperator();
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

	// ── Validation ───────────────────────────────────────────────────────────

	@Nested
	class Validation {

		@Test
		void throwsWhenConclusionLabelMissing() {
			var a = buildJustification("a", "C", "S", "E");
			assertThatThrownBy(() -> assemble.apply("r", List.of(a),
					Map.of("strategyLabel", "s")))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("conclusionLabel");
		}

		@Test
		void throwsWhenStrategyLabelMissing() {
			var a = buildJustification("a", "C", "S", "E");
			assertThatThrownBy(() -> assemble.apply("r", List.of(a),
					Map.of("conclusionLabel", "c")))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("strategyLabel");
		}
	}
}
