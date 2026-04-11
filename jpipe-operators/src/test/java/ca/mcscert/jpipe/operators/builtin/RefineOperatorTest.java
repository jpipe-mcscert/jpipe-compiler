package ca.mcscert.jpipe.operators.builtin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.model.Justification;
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

class RefineOperatorTest {

	private ExecutionEngine engine;
	private RefineOperator refine;

	@BeforeEach
	void setUp() {
		engine = new ExecutionEngine();
		refine = new RefineOperator();
	}

	// ── fixtures ─────────────────────────────────────────────────────────────

	/**
	 * Builds the "minimal" model: conclusion c ← strategy s ← evidence e.
	 */
	private Justification buildMinimal() {
		List<Command> cmds = new ArrayList<>();
		cmds.add(new CreateJustification("minimal"));
		cmds.add(new CreateConclusion("minimal", "c", "A conclusion"));
		cmds.add(new CreateStrategy("minimal", "s", "A strategy"));
		cmds.add(new CreateEvidence("minimal", "e", "An evidence"));
		cmds.add(new AddSupport("minimal", "c", "s"));
		cmds.add(new AddSupport("minimal", "s", "e"));
		Unit unit = engine.spawn("src", cmds);
		return (Justification) unit.get("minimal");
	}

	/**
	 * Builds the "refinement" model: conclusion c ← strategy s ← evidence e.
	 */
	private Justification buildRefinement() {
		List<Command> cmds = new ArrayList<>();
		cmds.add(new CreateJustification("refinement"));
		cmds.add(
				new CreateConclusion("refinement", "c", "an evidence is true"));
		cmds.add(new CreateStrategy("refinement", "s", "A strategy"));
		cmds.add(new CreateEvidence("refinement", "e", "An evidence"));
		cmds.add(new AddSupport("refinement", "c", "s"));
		cmds.add(new AddSupport("refinement", "s", "e"));
		Unit unit = engine.spawn("src", cmds);
		return (Justification) unit.get("refinement");
	}

	// ── tests
	// ─────────────────────────────────────────────────────────────────

	@Nested
	class ResultStructure {

		@Test
		void resultModelIsAJustification() {
			var minimal = buildMinimal();
			var refinement = buildRefinement();
			List<Command> cmds = refine.apply("refined",
					List.of(minimal, refinement), Map.of("hook", "minimal/e"));
			Unit unit = engine.spawn("out", cmds);
			assertThat(unit.findModel("refined")).isPresent();
			assertThat(unit.get("refined")).isInstanceOf(Justification.class);
		}

		@Test
		void hookElementAndRefinementConclusionMergeIntoSubConclusion() {
			var minimal = buildMinimal();
			var refinement = buildRefinement();
			List<Command> cmds = refine.apply("refined",
					List.of(minimal, refinement), Map.of("hook", "minimal/e"));
			Unit unit = engine.spawn("out", cmds);
			Justification result = (Justification) unit.get("refined");

			assertThat(result.subConclusions()).hasSize(1);
			SubConclusion hook = result.subConclusions().get(0);
			assertThat(hook.id()).isEqualTo(RefineOperator.HOOK_ID);
			assertThat(hook.label()).isEqualTo("an evidence is true");
		}

		@Test
		void nonMergedElementsGetSourcePrefixedIds() {
			var minimal = buildMinimal();
			var refinement = buildRefinement();
			List<Command> cmds = refine.apply("refined",
					List.of(minimal, refinement), Map.of("hook", "minimal/e"));
			Unit unit = engine.spawn("out", cmds);
			Justification result = (Justification) unit.get("refined");

			// Conclusion from minimal
			assertThat(result.conclusion()).isPresent();
			assertThat(result.conclusion().get().id()).isEqualTo("minimal:c");

			// Strategies from both sources
			assertThat(result.strategies()).extracting(Strategy::id)
					.containsExactlyInAnyOrder("minimal:s", "refinement:s");

			// Evidence from refinement only (minimal's evidence is the hook)
			assertThat(result.evidence()).hasSize(1);
			assertThat(result.evidence().get(0).id()).isEqualTo("refinement:e");
		}

		@Test
		void fourEdgesAreReconstructed() {
			var minimal = buildMinimal();
			var refinement = buildRefinement();
			List<Command> cmds = refine.apply("refined",
					List.of(minimal, refinement), Map.of("hook", "minimal/e"));
			Unit unit = engine.spawn("out", cmds);
			Justification result = (Justification) unit.get("refined");

			// minimal:s supports minimal:c (conclusion)
			assertThat(result.conclusion().flatMap(Conclusion::getSupport))
					.isPresent().get().extracting(Strategy::id)
					.isEqualTo("minimal:s");

			// hook supports minimal:s (was: e supports s in minimal)
			Strategy minimalS = result.strategies().stream()
					.filter(s -> s.id().equals("minimal:s")).findFirst()
					.orElseThrow();
			assertThat(minimalS.getSupports()).hasSize(1);
			assertThat(((SubConclusion) minimalS.getSupports().get(0)).id())
					.isEqualTo(RefineOperator.HOOK_ID);

			// refinement:s supports hook (was: s supports c in refinement)
			SubConclusion hook = result.subConclusions().get(0);
			assertThat(hook.getSupport()).isPresent();
			assertThat(hook.getSupport().get().id()).isEqualTo("refinement:s");

			// refinement:e supports refinement:s
			Strategy refinementS = result.strategies().stream()
					.filter(s -> s.id().equals("refinement:s")).findFirst()
					.orElseThrow();
			assertThat(refinementS.getSupports()).hasSize(1);
			assertThat(refinementS.getSupports().get(0).toString())
					.contains("refinement:e");
		}

		@Test
		void noDuplicateEdgesWhenBothSourcesShareEdgeAfterMerge() {
			var minimal = buildMinimal();
			var refinement = buildRefinement();
			List<Command> cmds = refine.apply("refined",
					List.of(minimal, refinement), Map.of("hook", "minimal/e"));

			long addSupportCount = cmds.stream()
					.filter(AddSupport.class::isInstance).count();
			assertThat(addSupportCount).isEqualTo(4);
		}
	}

	@Nested
	class Validation {

		@Test
		void throwsWhenHookArgumentMissing() {
			var minimal = buildMinimal();
			var refinement = buildRefinement();
			assertThatThrownBy(() -> refine.apply("refined",
					List.of(minimal, refinement), Map.of()))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("hook");
		}

		@Test
		void throwsWhenHookFormatHasNoSlash() {
			var minimal = buildMinimal();
			var refinement = buildRefinement();
			assertThatThrownBy(() -> refine.apply("refined",
					List.of(minimal, refinement), Map.of("hook", "e")))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("modelName/elementId");
		}

		@Test
		void throwsWhenHookModelNotAmongSources() {
			var minimal = buildMinimal();
			var refinement = buildRefinement();
			assertThatThrownBy(() -> refine.apply("refined",
					List.of(minimal, refinement), Map.of("hook", "unknown/e")))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("unknown");
		}

		@Test
		void throwsWhenSourceCountIsNotTwo() {
			var minimal = buildMinimal();
			assertThatThrownBy(() -> refine.apply("refined", List.of(minimal),
					Map.of("hook", "minimal/e")))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("2 sources");
		}
	}
}
