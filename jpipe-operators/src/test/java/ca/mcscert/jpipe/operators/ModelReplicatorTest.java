package ca.mcscert.jpipe.operators;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateEvidence;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateSubConclusion;
import ca.mcscert.jpipe.commands.linking.AddSupport;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModelReplicatorTest {

	private ExecutionEngine engine;

	@BeforeEach
	void setUp() {
		engine = new ExecutionEngine();
	}

	// ── fixtures ────────────────────────────────────────────────────────────

	/** Builds a simple justification: conclusion ← strategy ← evidence. */
	private Justification simpleJustification(String name) {
		Unit unit = engine.spawn("src",
				List.of(new CreateJustification(name),
						new CreateConclusion(name, "c", "the conclusion"),
						new CreateStrategy(name, "s", "the strategy"),
						new CreateEvidence(name, "e", "the evidence"),
						new AddSupport(name, "c", "s"),
						new AddSupport(name, "s", "e")));
		return (Justification) unit.get(name);
	}

	// ── tests ───────────────────────────────────────────────────────────────

	@Nested
	class WithPrefix {

		@Test
		void replicatesConclusion() {
			Justification src = simpleJustification("j");
			Unit result = engine.spawn("out",
					List.of(new CreateJustification("out")));
			engine.enrich(result, ModelReplicator.replicate("out", src, "j"));

			assertThat(result.get("out").conclusion()).isPresent().get()
					.extracting(Conclusion::id).isEqualTo("j:c");
		}

		@Test
		void replicatesStrategyWithQualifiedId() {
			Justification src = simpleJustification("j");
			Unit result = engine.spawn("out",
					List.of(new CreateJustification("out")));
			engine.enrich(result, ModelReplicator.replicate("out", src, "j"));

			assertThat(result.get("out").strategies()).extracting(Strategy::id)
					.containsExactly("j:s");
		}

		@Test
		void replicatesEvidenceWithQualifiedId() {
			Justification src = simpleJustification("j");
			Unit result = engine.spawn("out",
					List.of(new CreateJustification("out")));
			engine.enrich(result, ModelReplicator.replicate("out", src, "j"));

			assertThat(result.get("out").evidence()).extracting(Evidence::id)
					.containsExactly("j:e");
		}

		@Test
		void rewiresSupportEdges() {
			Justification src = simpleJustification("j");
			Unit result = engine.spawn("out",
					List.of(new CreateJustification("out")));
			engine.enrich(result, ModelReplicator.replicate("out", src, "j"));

			Justification out = (Justification) result.get("out");
			assertThat(out.conclusion().flatMap(Conclusion::getSupport))
					.isPresent().get().extracting(Strategy::id)
					.isEqualTo("j:s");
		}

		@Test
		void doesNotMutateSource() {
			Justification src = simpleJustification("j");
			int originalSize = src.getElements().size();
			Unit result = engine.spawn("out",
					List.of(new CreateJustification("out")));
			engine.enrich(result, ModelReplicator.replicate("out", src, "j"));

			assertThat(src.getElements()).hasSize(originalSize);
		}
	}

	@Nested
	class WithoutPrefix {

		@Test
		void keepsOriginalIds() {
			Justification src = simpleJustification("j");
			Unit result = engine.spawn("out",
					List.of(new CreateJustification("out")));
			engine.enrich(result, ModelReplicator.replicate("out", src, ""));

			assertThat(result.get("out").conclusion()).isPresent().get()
					.extracting(Conclusion::id).isEqualTo("c");
			assertThat(result.get("out").strategies()).extracting(Strategy::id)
					.containsExactly("s");
		}

		@Test
		void nullPrefixKeepsOriginalIds() {
			Justification src = simpleJustification("j");
			Unit result = engine.spawn("out",
					List.of(new CreateJustification("out")));
			engine.enrich(result, ModelReplicator.replicate("out", src, null));

			assertThat(result.get("out").conclusion()).isPresent().get()
					.extracting(Conclusion::id).isEqualTo("c");
		}
	}

	@Nested
	class WithSubConclusion {

		/**
		 * Builds: conclusion c ← strategy s ← sub-conclusion sc ← strategy s2
		 */
		private Justification justificationWithSubConclusion(String name) {
			Unit unit = engine.spawn("src",
					List.of(new CreateJustification(name),
							new CreateConclusion(name, "c", "the conclusion"),
							new CreateStrategy(name, "s", "the strategy"),
							new CreateSubConclusion(name, "sc",
									"the sub-conclusion"),
							new CreateStrategy(name, "s2", "inner strategy"),
							new CreateEvidence(name, "e", "evidence"),
							new AddSupport(name, "c", "s"),
							new AddSupport(name, "s", "sc"),
							new AddSupport(name, "sc", "s2"),
							new AddSupport(name, "s2", "e")));
			return (Justification) unit.get(name);
		}

		@Test
		void replicatesSubConclusionWithPrefix() {
			Justification src = justificationWithSubConclusion("j");
			Unit result = engine.spawn("out",
					List.of(new CreateJustification("out")));
			engine.enrich(result, ModelReplicator.replicate("out", src, "j"));

			assertThat(result.get("out").subConclusions())
					.extracting(SubConclusion::id).containsExactly("j:sc");
		}

		@Test
		void replicatesSubConclusionSupportEdge() {
			Justification src = justificationWithSubConclusion("j");
			Unit result = engine.spawn("out",
					List.of(new CreateJustification("out")));
			engine.enrich(result, ModelReplicator.replicate("out", src, "j"));

			Justification out = (Justification) result.get("out");
			SubConclusion sc = out.subConclusions().get(0);
			assertThat(sc.getSupport()).isPresent().get()
					.extracting(Strategy::id).isEqualTo("j:s2");
		}
	}

	@Nested
	class AlreadyQualifiedIds {

		@Test
		void alreadyQualifiedIdIsNotDoubleQualified() {
			// Build a justification whose strategy already has a qualified id
			Unit unit = engine.spawn("src",
					List.of(new CreateJustification("j"),
							new CreateConclusion("j", "c", "C"),
							new CreateStrategy("j", "ns:s", "S"),
							new AddSupport("j", "c", "ns:s")));
			Justification src = (Justification) unit.get("j");

			Unit result = engine.spawn("out",
					List.of(new CreateJustification("out")));
			engine.enrich(result, ModelReplicator.replicate("out", src, "j"));

			// "ns:s" already contains ":" so prefix "j" must not be prepended
			assertThat(result.get("out").strategies()).extracting(Strategy::id)
					.containsExactly("ns:s");
		}
	}

	@Nested
	class CommandList {

		@Test
		void isUnmodifiable() {
			Justification src = simpleJustification("j");
			List<Command> commands = ModelReplicator.replicate("out", src, "j");
			assertThat(commands).isUnmodifiable();
		}

		@Test
		void hasCorrectCommandCount() {
			// 1 CreateConclusion + 1 CreateStrategy + 1 CreateEvidence
			// + 1 AddSupport(c→s) + 1 AddSupport(s→e) = 5
			Justification src = simpleJustification("j");
			List<Command> commands = ModelReplicator.replicate("out", src, "j");
			assertThat(commands).hasSize(5);
		}
	}
}
