package ca.mcscert.jpipe.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.RegisterAlias;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.operators.equivalences.SameShortId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CompositionOperatorTest {

	private ExecutionEngine engine;

	@BeforeEach
	void setUp() {
		engine = new ExecutionEngine();
	}

	// ── minimal concrete operator for testing ───────────────────────────────

	/**
	 * Operator that groups by short id (SameShortId), keeps the first member's
	 * short id as the new id, and creates the same element type as the first
	 * member (Evidence for simplicity — real operators would be type-aware).
	 * Produces a Justification result model.
	 */
	private static final class TestOperator extends CompositionOperator {

		@Override
		protected EquivalenceRelation equivalenceRelation(
				List<JustificationModel<?>> sources,
				Map<String, String> arguments) {
			return new SameShortId();
		}

		@Override
		protected MergeFunction mergeFunction(
				List<JustificationModel<?>> sources,
				Map<String, String> arguments) {
			return (resultName, group, aliases) -> {
				// Use the first member's label and derive id from first
				// member's short id
				var first = group.members().get(0).element();
				String newId = shortId(first.id());
				// Register qualified old ids so Phase 2 qualified lookup works
				aliases.register(newId,
						group.members().stream().map(
								se -> qualId(se.source(), se.element().id()))
								.toList());
				return switch (first) {
					case ca.mcscert.jpipe.model.elements.Conclusion c ->
						List.of(new CreateConclusion(resultName, newId,
								c.label()));
					case Strategy s -> List.of(
							new CreateStrategy(resultName, newId, s.label()));
					case Evidence e -> List.of(
							new CreateEvidence(resultName, newId, e.label()));
					default -> List.of(new CreateEvidence(resultName, newId,
							first.label()));
				};
			};
		}

		@Override
		protected Command createResultModel(String name,
				SourceLocation location, List<JustificationModel<?>> sources,
				Map<String, String> arguments) {
			return new CreateJustification(name);
		}

		private static String shortId(String id) {
			int colon = id.lastIndexOf(':');
			return colon >= 0 ? id.substring(colon + 1) : id;
		}

		private static String qualId(JustificationModel<?> source, String id) {
			return id.contains(":") ? id : source.getName() + ":" + id;
		}
	}

	/** Operator that requires an argument key "mode". */
	private static final class RequiresArgOperator extends CompositionOperator {

		@Override
		protected Set<String> requiredArguments() {
			return Set.of("mode");
		}

		@Override
		protected EquivalenceRelation equivalenceRelation(
				List<JustificationModel<?>> sources,
				Map<String, String> arguments) {
			return (a, b) -> false;
		}

		@Override
		protected MergeFunction mergeFunction(
				List<JustificationModel<?>> sources,
				Map<String, String> arguments) {
			return (r, g, a) -> List.of();
		}

		@Override
		protected Command createResultModel(String name,
				SourceLocation location, List<JustificationModel<?>> sources,
				Map<String, String> arguments) {
			return new CreateJustification(name);
		}
	}

	// ── fixtures ────────────────────────────────────────────────────────────

	private Justification buildJustification(String name,
			List<Command> extraCommands) {
		List<Command> all = new java.util.ArrayList<>();
		all.add(new CreateJustification(name));
		all.addAll(extraCommands);
		Unit unit = engine.spawn("src", all);
		return (Justification) unit.get(name);
	}

	// ── tests ───────────────────────────────────────────────────────────────

	@Nested
	class Apply {

		@Test
		void resultModelIsCreated() {
			Justification j = buildJustification("j",
					List.of(new CreateConclusion("j", "c", "the conclusion")));

			List<Command> commands = new TestOperator().apply("result",
					List.of(j), Map.of());
			Unit unit = engine.spawn("out", commands);

			assertThat(unit.findModel("result")).isPresent();
		}

		@Test
		void equivalentElementsMergedIntoSingleElement() {
			// Two justifications both have a strategy with short id "s"
			Justification j1 = buildJustification("j1",
					List.of(new CreateConclusion("j1", "c", "C"),
							new CreateStrategy("j1", "j1:s", "shared strategy"),
							new AddSupport("j1", "c", "j1:s")));
			Justification j2 = buildJustification("j2",
					List.of(new CreateConclusion("j2", "c", "C"),
							new CreateStrategy("j2", "j2:s", "shared strategy"),
							new AddSupport("j2", "c", "j2:s")));

			List<Command> commands = new TestOperator().apply("result",
					List.of(j1, j2), Map.of());
			Unit unit = engine.spawn("out", commands);

			// Both "j1:s" and "j2:s" merge to "s" — only one strategy in result
			assertThat(unit.get("result").strategies()).hasSize(1)
					.extracting(Strategy::id).containsExactly("s");
		}

		@Test
		void supportEdgesAreReconstructed() {
			Justification j = buildJustification("j",
					List.of(new CreateConclusion("j", "c", "C"),
							new CreateStrategy("j", "j:s", "strategy"),
							new CreateEvidence("j", "j:e", "evidence"),
							new AddSupport("j", "c", "j:s"),
							new AddSupport("j", "j:s", "j:e")));

			List<Command> commands = new TestOperator().apply("result",
					List.of(j), Map.of());
			Unit unit = engine.spawn("out", commands);

			Justification result = (Justification) unit.get("result");
			assertThat(result.conclusion().flatMap(Conclusion::getSupport))
					.isPresent().get().extracting(Strategy::id).isEqualTo("s");
			assertThat(result.strategies().get(0).getSupports())
					.extracting(leaf -> ((Evidence) leaf).id())
					.containsExactly("e");
		}

		@Test
		void duplicateEdgesFromMultipleSourcesNotDuplicated() {
			// Both sources have the same conclusion→strategy edge
			Justification j1 = buildJustification("j1",
					List.of(new CreateConclusion("j1", "c", "C"),
							new CreateStrategy("j1", "j1:s", "S"),
							new AddSupport("j1", "c", "j1:s")));
			Justification j2 = buildJustification("j2",
					List.of(new CreateConclusion("j2", "c", "C"),
							new CreateStrategy("j2", "j2:s", "S"),
							new AddSupport("j2", "c", "j2:s")));

			List<Command> commands = new TestOperator().apply("result",
					List.of(j1, j2), Map.of());

			long addSupportCount = commands.stream()
					.filter(AddSupport.class::isInstance).count();
			assertThat(addSupportCount).isEqualTo(1);
		}

		@Test
		void aliasCommandsEmittedForEachOldId() {
			// Use distinct qualified ids so the alias registry gets two
			// entries:
			// "j1:c" -> "c" and "j2:c" -> "c"
			Justification j1 = buildJustification("j1",
					List.of(new CreateConclusion("j1", "j1:c", "C")));
			Justification j2 = buildJustification("j2",
					List.of(new CreateConclusion("j2", "j2:c", "C")));

			List<Command> commands = new TestOperator().apply("result",
					List.of(j1, j2), Map.of());

			assertThat(commands).filteredOn(RegisterAlias.class::isInstance)
					.hasSize(2);
		}

		@Test
		void returnedListIsUnmodifiable() {
			Justification j = buildJustification("j",
					List.of(new CreateConclusion("j", "c", "C")));
			List<Command> commands = new TestOperator().apply("result",
					List.of(j), Map.of());
			assertThat(commands).isUnmodifiable();
		}
	}

	@Nested
	class RequiredArguments {

		@Test
		void throwsWhenRequiredKeyMissing() {
			Justification j = buildJustification("j",
					List.of(new CreateConclusion("j", "c", "C")));
			assertThatThrownBy(() -> new RequiresArgOperator().apply("result",
					List.of(j), Map.of()))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("mode");
		}

		@Test
		void doesNotThrowWhenRequiredKeyPresent() {
			Justification j = buildJustification("j",
					List.of(new CreateConclusion("j", "c", "C")));
			assertThat(new RequiresArgOperator().apply("result", List.of(j),
					Map.of("mode", "strict"))).isNotNull();
		}
	}
}
