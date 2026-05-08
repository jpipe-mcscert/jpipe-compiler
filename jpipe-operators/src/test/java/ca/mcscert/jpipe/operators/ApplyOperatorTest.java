package ca.mcscert.jpipe.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.operators.equivalences.SameLabel;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ApplyOperatorTest {

	// ── minimal concrete operator ────────────────────────────────────────────

	private static final class NoOpOperator extends CompositionOperator {

		private final ModelKind kind;

		NoOpOperator(ModelKind kind) {
			this.kind = kind;
		}

		@Override
		protected EquivalenceRelation equivalenceRelation(
				List<JustificationModel<?>> sources,
				Map<String, String> arguments,
				Map<String, String> knownAliases) {
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

		@Override
		public ModelKind resultKind(List<JustificationModel<?>> sources,
				Map<String, String> args) {
			return kind;
		}
	}

	// ── setup ────────────────────────────────────────────────────────────────

	private OperatorRegistry operators;
	private UnificationEquivalenceRegistry unification;
	private ExecutionEngine engine;

	@BeforeEach
	void setUp() {
		operators = new OperatorRegistry();
		operators.register("noop", new NoOpOperator(ModelKind.JUSTIFICATION));
		operators.register("template-op", new NoOpOperator(ModelKind.TEMPLATE));
		unification = new UnificationEquivalenceRegistry();
		unification.register("sameLabel", new SameLabel());
		engine = new ExecutionEngine();
	}

	private Unit unitWithModels(String... names) {
		List<Command> cmds = new java.util.ArrayList<>();
		for (String name : names) {
			cmds.add(new CreateJustification(name));
		}
		return engine.spawn("src", cmds);
	}

	private ApplyOperator applyOperator(String resultName, String opName,
			List<String> sources, Map<String, String> args,
			ModelKind declared) {
		OperatorCallConfig config = new OperatorCallConfig(resultName, opName,
				sources, args, SourceLocation.UNKNOWN, declared);
		return new ApplyOperator(config, operators, unification);
	}

	// ── accessors ────────────────────────────────────────────────────────────

	@Nested
	class Accessors {

		@Test
		void resultName() {
			var op = applyOperator("out", "noop", List.of(), Map.of(),
					ModelKind.JUSTIFICATION);
			assertThat(op.resultName()).isEqualTo("out");
		}

		@Test
		void operatorName() {
			var op = applyOperator("out", "noop", List.of(), Map.of(),
					ModelKind.JUSTIFICATION);
			assertThat(op.operatorName()).isEqualTo("noop");
		}

		@Test
		void sourceNames() {
			var op = applyOperator("out", "noop", List.of("a", "b"), Map.of(),
					ModelKind.JUSTIFICATION);
			assertThat(op.sourceNames()).containsExactly("a", "b");
		}

		@Test
		void arguments() {
			var op = applyOperator("out", "noop", List.of(),
					Map.of("key", "val"), ModelKind.JUSTIFICATION);
			assertThat(op.arguments()).containsEntry("key", "val");
		}

		@Test
		void location() {
			var op = applyOperator("out", "noop", List.of(), Map.of(),
					ModelKind.JUSTIFICATION);
			assertThat(op.location()).isEqualTo(SourceLocation.UNKNOWN);
		}

		@Test
		void toStringContainsResultNameOperatorNameAndSources() {
			var op = applyOperator("out", "noop", List.of("a", "b"), Map.of(),
					ModelKind.JUSTIFICATION);
			assertThat(op).hasToString("applyOperator('out', 'noop', [a, b]).");
		}
	}

	// ── condition ────────────────────────────────────────────────────────────

	@Nested
	class Condition {

		@Test
		void trueWhenAllSourcesPresent() {
			Unit unit = unitWithModels("a", "b");
			var op = applyOperator("out", "noop", List.of("a", "b"), Map.of(),
					ModelKind.JUSTIFICATION);
			assertThat(op.condition().test(unit)).isTrue();
		}

		@Test
		void falseWhenSourceMissing() {
			Unit unit = unitWithModels("a");
			var op = applyOperator("out", "noop", List.of("a", "b"), Map.of(),
					ModelKind.JUSTIFICATION);
			assertThat(op.condition().test(unit)).isFalse();
		}

		@Test
		void trueWithNoSources() {
			Unit unit = unitWithModels();
			var op = applyOperator("out", "noop", List.of(), Map.of(),
					ModelKind.JUSTIFICATION);
			assertThat(op.condition().test(unit)).isTrue();
		}
	}

	// ── expand ───────────────────────────────────────────────────────────────

	@Nested
	class Expand {

		@Test
		void happyPathCreatesResultModel() {
			Unit unit = unitWithModels("a");
			var op = applyOperator("out", "noop", List.of("a"), Map.of(),
					ModelKind.JUSTIFICATION);
			List<Command> cmds = op.expand(unit);
			Unit result = engine.spawn("out2", new java.util.ArrayList<>(cmds));
			assertThat(result.findModel("out")).isPresent();
		}

		@Test
		void unknownOperatorThrows() {
			Unit unit = unitWithModels("a");
			var op = applyOperator("out", "no-such-op", List.of("a"), Map.of(),
					ModelKind.JUSTIFICATION);
			assertThatThrownBy(() -> op.expand(unit))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("no-such-op");
		}

		@Test
		void kindMismatchThrows() {
			Unit unit = unitWithModels("a");
			// operator returns TEMPLATE but declared kind is JUSTIFICATION
			var op = applyOperator("out", "template-op", List.of("a"), Map.of(),
					ModelKind.JUSTIFICATION);
			assertThatThrownBy(() -> op.expand(unit))
					.isInstanceOf(InvalidOperatorCallException.class)
					.hasMessageContaining("template")
					.hasMessageContaining("justification");
		}
	}
}
