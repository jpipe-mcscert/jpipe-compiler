package ca.mcscert.jpipe.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.commands.linking.RegisterAlias;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.operators.equivalences.SameLabel;
import ca.mcscert.jpipe.operators.equivalences.SameShortId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class UnifierTest {

	/** Label shared by the members of a merged group. */
	private static final String SHARED = "A shared claim";

	/** Name of the model under test; {@link #model()} for instance contexts. */
	private static final String MODEL = "m";

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

	// ── groups mixing element kinds
	// ───────────────────────────────────────────────

	@Nested
	class MixedKinds {

		/**
		 * A group mixing a sub-conclusion and an evidence carrying the same
		 * claim: one source argued it, the other still asserts it.
		 */
		private List<Command> argued(boolean subConclusionFirst) {
			List<Command> elements = subConclusionFirst
					? List.of(new CreateSubConclusion(model(), "sc", SHARED),
							new CreateEvidence(model(), "e", SHARED))
					: List.of(new CreateEvidence(model(), "e", SHARED),
							new CreateSubConclusion(model(), "sc", SHARED));
			List<Command> cmds = new ArrayList<>(
					List.of(new CreateJustification(model())));
			cmds.addAll(elements);
			return cmds;
		}

		@ParameterizedTest(name = "sub-conclusion listed first: {0}")
		@ValueSource(booleans = {true, false})
		void subConclusionWinsWhateverTheOrder(boolean subConclusionFirst) {
			List<Command> result = unifier.unify(model(),
					argued(subConclusionFirst), Map.of());

			assertThat(result).filteredOn(Unifier::isElement)
					.filteredOn(cmd -> "unified_0".equals(Unifier.idOf(cmd)))
					.singleElement().isInstanceOf(CreateSubConclusion.class)
					.extracting(Unifier::labelOf).isEqualTo(SHARED);
		}

		@Test
		void mergedElementKeepsThePositionOfItsFirstMember() {
			// The evidence is listed first, so unified_0 takes its slot even
			// though the sub-conclusion is the one it is built from.
			List<Command> cmds = argued(false);
			cmds.add(new CreateEvidence(model(), "last", "Something else"));

			List<Command> result = unifier.unify(model(), cmds, Map.of());

			assertThat(result).filteredOn(Unifier::isElement)
					.extracting(Unifier::idOf)
					.containsExactly("unified_0", "last");
		}

		@Test
		void mergedSubConclusionCanCarryAStrategyAndSupportAnother() {
			// The reason sub-conclusion must win: it is both supported by the
			// strategy that argues it, and a supporter of the strategy that
			// used to lean on the evidence.
			List<Command> cmds = argued(false);
			cmds.addAll(List.of(new CreateStrategy(model(), "arguing", "why"),
					new CreateStrategy(model(), "leaning", "because"),
					new AddSupport(model(), "sc", "arguing"),
					new AddSupport(model(), "leaning", "e")));

			Unit unit = engine.spawn("test",
					unifier.unify(model(), cmds, Map.of()));
			Justification result = (Justification) unit.get(model());

			assertThat(result.subConclusions()).singleElement()
					.satisfies(sc -> assertThat(sc.id()).isEqualTo("unified_0"))
					.satisfies(sc -> assertThat(sc.getSupport()).get()
							.extracting(JustificationElement::id)
							.isEqualTo("arguing"));
		}

		static Stream<Arguments> incomparablePairs() {
			return Stream.of(
					Arguments.of("strategy and evidence",
							new CreateStrategy(MODEL, "s", SHARED),
							new CreateEvidence(MODEL, "e", SHARED)),
					Arguments.of("conclusion and evidence",
							new CreateConclusion(MODEL, "c", SHARED),
							new CreateEvidence(MODEL, "e", SHARED)),
					Arguments.of("conclusion and sub-conclusion",
							new CreateConclusion(MODEL, "c", SHARED),
							new CreateSubConclusion(MODEL, "sc", SHARED)),
					Arguments.of("evidence and abstract support",
							new CreateEvidence(MODEL, "e", SHARED),
							new CreateAbstractSupport(MODEL, "as", SHARED)),
					Arguments.of("sub-conclusion and abstract support",
							new CreateSubConclusion(MODEL, "sc", SHARED),
							new CreateAbstractSupport(MODEL, "as", SHARED)));
		}

		@ParameterizedTest(name = "{0}")
		@MethodSource("incomparablePairs")
		void incomparableKindsAreRejected(String label, Command first,
				Command second) {
			List<Command> cmds = List.of(new CreateJustification(model()),
					first, second);
			Map<String, String> args = Map.of();
			assertThatThrownBy(() -> unifier.unify(MODEL, cmds, args))
					.isInstanceOf(IncompatibleUnificationException.class)
					.hasMessageContaining("cannot unify")
					.hasMessageContaining(SHARED)
					.hasMessageContaining("element kinds are incompatible")
					.hasMessageContaining(Unifier.UNIFY_EXCLUDE_KEY);
		}

		@Test
		void theRejectionMessageIsTheSameWhateverTheOrder() {
			Command strategy = new CreateStrategy(model(), "s", SHARED);
			Command evidence = new CreateEvidence(model(), "e", SHARED);
			List<Command> oneWay = List.of(new CreateJustification(model()),
					strategy, evidence);
			List<Command> theOther = List.of(new CreateJustification(model()),
					evidence, strategy);

			String first = messageOf(oneWay);
			String second = messageOf(theOther);

			assertThat(first).isEqualTo(second);
		}

		private String messageOf(List<Command> cmds) {
			try {
				unifier.unify(model(), cmds, Map.of());
				throw new AssertionError("expected the group to be rejected");
			} catch (IncompatibleUnificationException e) {
				return e.getMessage();
			}
		}

		@Test
		void homogeneousGroupStillTakesTheFirstMembersLabel() {
			// sameShortId ignores labels, so a group can hold two evidence
			// elements with different labels: the first one still wins.
			UnificationEquivalenceRegistry registry = new UnificationEquivalenceRegistry();
			registry.register("sameShortId", new SameShortId());
			List<Command> cmds = List.of(new CreateJustification(model()),
					new CreateEvidence(model(), "a:e", "first label"),
					new CreateEvidence(model(), "b:e", "second label"));

			List<Command> result = new Unifier(registry).unify(model(), cmds,
					Map.of("unifyBy", "sameShortId"));

			assertThat(result).filteredOn(Unifier::isElement).singleElement()
					.extracting(Unifier::labelOf).isEqualTo("first label");
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
		return MODEL;
	}
}
