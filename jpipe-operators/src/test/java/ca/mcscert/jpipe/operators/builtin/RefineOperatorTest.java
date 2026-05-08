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
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
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

	/**
	 * Builds a model whose element "e" has been unified into "unified_0",
	 * simulating a post-assemble source where the original id became an alias.
	 */
	private Justification buildUnifiedSource() {
		List<Command> cmds = new ArrayList<>();
		cmds.add(new CreateJustification("src"));
		cmds.add(new CreateConclusion("src", "c", "A conclusion"));
		cmds.add(new CreateStrategy("src", "s", "A strategy"));
		cmds.add(new CreateEvidence("src", "unified_0", "An evidence"));
		cmds.add(new AddSupport("src", "c", "s"));
		cmds.add(new AddSupport("src", "s", "unified_0"));
		Unit unit = engine.spawn("out", cmds);
		return (Justification) unit.get("src");
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
					List.of(minimal, refinement), Map.of("hook", "e"));
			Unit unit = engine.spawn("out", cmds);
			assertThat(unit.findModel("refined")).isPresent();
			assertThat(unit.get("refined")).isInstanceOf(Justification.class);
		}

		@Test
		void hookElementAndRefinementConclusionMergeIntoSubConclusion() {
			var minimal = buildMinimal();
			var refinement = buildRefinement();
			List<Command> cmds = refine.apply("refined",
					List.of(minimal, refinement), Map.of("hook", "e"));
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
					List.of(minimal, refinement), Map.of("hook", "e"));
			Unit unit = engine.spawn("out", cmds);
			Justification result = (Justification) unit.get("refined");

			assertThat(result.conclusion()).isPresent();
			assertThat(result.conclusion().get().id()).isEqualTo("minimal:c");

			assertThat(result.strategies()).extracting(Strategy::id)
					.containsExactlyInAnyOrder("minimal:s", "refinement:s");

			assertThat(result.evidence()).hasSize(1);
			assertThat(result.evidence().get(0).id()).isEqualTo("refinement:e");
		}

		@Test
		void fourEdgesAreReconstructed() {
			var minimal = buildMinimal();
			var refinement = buildRefinement();
			List<Command> cmds = refine.apply("refined",
					List.of(minimal, refinement), Map.of("hook", "e"));
			Unit unit = engine.spawn("out", cmds);
			Justification result = (Justification) unit.get("refined");

			assertThat(result.conclusion().flatMap(Conclusion::getSupport))
					.isPresent().get().extracting(Strategy::id)
					.isEqualTo("minimal:s");

			Strategy minimalS = result.strategies().stream()
					.filter(s -> s.id().equals("minimal:s")).findFirst()
					.orElseThrow();
			assertThat(minimalS.getSupports()).hasSize(1);
			assertThat(((SubConclusion) minimalS.getSupports().get(0)).id())
					.isEqualTo(RefineOperator.HOOK_ID);

			SubConclusion hook = result.subConclusions().get(0);
			assertThat(hook.getSupport()).isPresent();
			assertThat(hook.getSupport().get().id()).isEqualTo("refinement:s");

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
					List.of(minimal, refinement), Map.of("hook", "e"));

			long addSupportCount = cmds.stream()
					.filter(AddSupport.class::isInstance).count();
			assertThat(addSupportCount).isEqualTo(4);
		}

		@Test
		void hookCanBeReferencedByAlias() {
			// Simulates refine(src, refinement) when "e" in src was unified
			// into "unified_0" by a prior composition step. The alias map
			// contains "src/e" → "unified_0".
			var src = buildUnifiedSource();
			var refinement = buildRefinement();
			Map<String, String> knownAliases = Map.of("src/e", "unified_0");

			List<Command> cmds = refine.apply("refined",
					List.of(src, refinement), Map.of("hook", "e"),
					SourceLocation.UNKNOWN, Map.of(), knownAliases);
			Unit unit = engine.spawn("out", cmds);
			Justification result = (Justification) unit.get("refined");

			assertThat(result.subConclusions()).hasSize(1);
			assertThat(result.subConclusions().get(0).id())
					.isEqualTo(RefineOperator.HOOK_ID);
			assertThat(result.subConclusions().get(0).label())
					.isEqualTo("an evidence is true");
		}
	}

	@Nested
	class Validation {

		@Test
		void throwsWhenHookArgumentMissing() {
			List<JustificationModel<?>> sources = List.of(buildMinimal(),
					buildRefinement());
			Map<String, String> args = Map.of();
			assertThatThrownBy(() -> refine.apply("refined", sources, args))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("hook");
		}

		@Test
		void throwsWhenSourceCountIsNotTwo() {
			List<JustificationModel<?>> sources = List.of(buildMinimal());
			Map<String, String> args = Map.of("hook", "e");
			assertThatThrownBy(() -> refine.apply("refined", sources, args))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("2 sources");
		}

		@Test
		void throwsWhenHookElementNotFoundInBaseModel() {
			List<JustificationModel<?>> sources = List.of(buildMinimal(),
					buildRefinement());
			Map<String, String> args = Map.of("hook", "nonexistent");
			assertThatThrownBy(() -> refine.apply("refined", sources, args))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("nonexistent")
					.hasMessageContaining("minimal");
		}
	}
}
