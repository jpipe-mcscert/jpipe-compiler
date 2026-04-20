package ca.mcscert.jpipe.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.RegisterAlias;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.operators.equivalences.SameLabel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UnifierTest {

	private Unifier unifier;
	private ExecutionEngine engine;

	@BeforeEach
	void setUp() {
		UnificationEquivalenceRegistry registry = new UnificationEquivalenceRegistry();
		registry.register("sameLabel", new SameLabel());
		unifier = new Unifier(registry);
		engine = new ExecutionEngine();
	}

	// ── helpers
	// ──────────────────────────────────────────────────────────────────

	/**
	 * Builds a minimal command list: model + conclusion + strategy + evidence.
	 */
	private List<Command> baseCommands(String model) {
		return new ArrayList<>(List.of(new CreateJustification(model),
				new CreateConclusion(model, "c", "A conclusion"),
				new CreateStrategy(model, "s", "A strategy"),
				new CreateEvidence(model, "e", "An evidence"),
				new AddSupport(model, "c", "s"),
				new AddSupport(model, "s", "e")));
	}

	// ── no-op cases
	// ──────────────────────────────────────────────────────────────────

	@Nested
	class NoOp {

		@Test
		void returnsCommandsUnchangedWhenNoEquivalentPairs() {
			List<Command> cmds = baseCommands("m");
			List<Command> result = unifier.unify("m", cmds, Map.of());
			assertThat(result).isEqualTo(cmds);
		}

		@Test
		void returnsUnmodifiableList() {
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification("m"),
							new CreateConclusion("m", "c1", "unique"),
							new CreateEvidence("m", "e2", "also unique")));
			List<Command> result = unifier.unify("m", cmds, Map.of());
			assertThat(result).isUnmodifiable();
		}
	}

	// ── merge cases
	// ──────────────────────────────────────────────────────────────────

	@Nested
	class Merge {

		@Test
		void twoElementsWithSameLabelMergeIntoUnified0() {
			// Two evidence elements with identical labels
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification("m"),
							new CreateStrategy(model(), "s1", "A strategy"),
							new CreateStrategy(model(), "s2",
									"Another strategy"),
							new CreateEvidence(model(), "e1", "shared label"),
							new CreateEvidence(model(), "e2", "shared label"),
							new AddSupport(model(), "s1", "e1"),
							new AddSupport(model(), "s2", "e2")));
			List<Command> result = unifier.unify(model(), cmds, Map.of());

			// No e1 or e2 in result; unified_0 replaces both
			assertThat(result.stream().filter(Unifier::isElement)
					.map(Unifier::idOf)).doesNotContain("e1", "e2")
					.contains("unified_0");
		}

		@Test
		void unifiedElementHasCorrectLabel() {
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification("m"),
							new CreateEvidence(model(), "e1", "shared"),
							new CreateEvidence(model(), "e2", "shared")));
			List<Command> result = unifier.unify(model(), cmds, Map.of());

			assertThat(result.stream().filter(Unifier::isElement)
					.map(Unifier::labelOf)).containsExactly("shared");
		}

		@Test
		void bothOriginalIdsAreAliasedToUnified0() {
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification("m"),
							new CreateEvidence(model(), "e1", "shared"),
							new CreateEvidence(model(), "e2", "shared")));
			List<Command> result = unifier.unify(model(), cmds, Map.of());

			List<RegisterAlias> aliases = result.stream()
					.filter(RegisterAlias.class::isInstance)
					.map(RegisterAlias.class::cast).toList();
			assertThat(aliases).hasSize(2);
			assertThat(aliases).extracting(RegisterAlias::oldId)
					.containsExactlyInAnyOrder("e1", "e2");
			assertThat(aliases).extracting(RegisterAlias::newId)
					.containsOnly("unified_0");
		}

		@Test
		void edgesAreRewrittenToUnifiedId() {
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification("m"),
							new CreateStrategy(model(), "s1", "strat1"),
							new CreateStrategy(model(), "s2", "strat2"),
							new CreateEvidence(model(), "e1", "shared"),
							new CreateEvidence(model(), "e2", "shared"),
							new AddSupport(model(), "s1", "e1"),
							new AddSupport(model(), "s2", "e2")));
			List<Command> result = unifier.unify(model(), cmds, Map.of());

			List<AddSupport> edges = result.stream()
					.filter(AddSupport.class::isInstance)
					.map(AddSupport.class::cast).toList();
			assertThat(edges).extracting(AddSupport::supporterId)
					.doesNotContain("e1", "e2")
					.contains("unified_0", "unified_0");
		}

		@Test
		void duplicateEdgesAreDeduplicatedAfterMerge() {
			// Two edges both become s→unified_0 after rewriting
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification("m"),
							new CreateStrategy(model(), "s", "A strategy"),
							new CreateEvidence(model(), "e1", "same"),
							new CreateEvidence(model(), "e2", "same"),
							new AddSupport(model(), "s", "e1"),
							new AddSupport(model(), "s", "e2")));
			List<Command> result = unifier.unify(model(), cmds, Map.of());

			long edgeCount = result.stream()
					.filter(AddSupport.class::isInstance)
					.map(AddSupport.class::cast)
					.filter(as -> "unified_0".equals(as.supporterId())).count();
			assertThat(edgeCount).isEqualTo(1);
		}

		@Test
		void twoGroupsGetIncrementingCounters() {
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification("m"),
							new CreateEvidence(model(), "e1", "group A"),
							new CreateEvidence(model(), "e2", "group A"),
							new CreateEvidence(model(), "e3", "group B"),
							new CreateEvidence(model(), "e4", "group B")));
			List<Command> result = unifier.unify(model(), cmds, Map.of());

			assertThat(result.stream().filter(Unifier::isElement)
					.map(Unifier::idOf))
					.containsExactlyInAnyOrder("unified_0", "unified_1");
		}

		@Test
		void mergedModelIsValidAndExecutable() {
			// e1 and e2 have same label; both support s. After unification
			// unified_0 replaces both and the duplicate edge is deduplicated.
			// Valid structure: c ← s ← e1, s ← e2 (both evidence → strategy)
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification("m"),
							new CreateConclusion("m", "c", "C"),
							new CreateStrategy("m", "s", "S"),
							new CreateEvidence("m", "e1", "shared"),
							new CreateEvidence("m", "e2", "shared"),
							new AddSupport("m", "c", "s"),
							new AddSupport("m", "s", "e1"),
							new AddSupport("m", "s", "e2")));
			List<Command> unified = unifier.unify("m", cmds, Map.of());
			Unit unit = engine.spawn("test", unified);
			Justification result = (Justification) unit.get("m");

			// Only one evidence in the model
			assertThat(result.evidence()).hasSize(1);
			Evidence ev = result.evidence().get(0);
			assertThat(ev.id()).isEqualTo("unified_0");
			assertThat(ev.label()).isEqualTo("shared");

			// unified_0 supports s (exactly once after deduplication)
			assertThat(result.strategies()).first()
					.extracting(str -> str.getSupports().stream()
							.map(l -> ((JustificationElement) l).id()).toList())
					.asInstanceOf(LIST).containsExactly("unified_0");
		}
	}

	// ── exclusion list
	// ────────────────────────────────────────────────────────────────

	@Nested
	class Exclusion {

		@Test
		void excludedElementIsNotMerged() {
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification("m"),
							new CreateEvidence(model(), "e1", "shared"),
							new CreateEvidence(model(), "e2", "shared")));
			// Exclude e1: the group {e1, e2} has e1 excluded, so e1 stays alone
			// and e2 also has nobody left to merge with → no unification
			List<Command> result = unifier.unify(model(), cmds,
					Map.of("unifyExclude", "e1"));
			assertThat(result).isEqualTo(cmds);
		}

		@Test
		void excludedElementDoesNotBlockOtherGroupsFromMerging() {
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification("m"),
							new CreateEvidence(model(), "e1", "shared"),
							new CreateEvidence(model(), "e2", "shared"),
							new CreateEvidence(model(), "e3", "shared"),
							new CreateEvidence(model(), "ex", "other")));
			// Exclude e1 → e2 and e3 still form a group and get merged
			List<Command> result = unifier.unify(model(), cmds,
					Map.of("unifyExclude", "e1"));
			assertThat(result.stream().filter(Unifier::isElement)
					.map(Unifier::idOf)).contains("e1", "unified_0")
					.doesNotContain("e2", "e3");
		}
	}

	// ── unknown unifyBy
	// ───────────────────────────────────────────────────────────────

	@Nested
	class Validation {

		@Test
		void unknownUnifyByThrowsInvalidOperatorCallException() {
			List<Command> cmds = List.of(new CreateJustification("m"));
			Map<String, String> args = Map.of("unifyBy", "bogus");
			assertThatThrownBy(() -> unifier.unify("m", cmds, args))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("bogus");
		}
	}

	private static String model() {
		return "m";
	}
}
