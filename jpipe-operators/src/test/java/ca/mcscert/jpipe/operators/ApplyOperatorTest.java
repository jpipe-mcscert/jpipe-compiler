package ca.mcscert.jpipe.operators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.model.Composition;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.operators.equivalences.SameLabel;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Justification;

class ApplyOperatorTest {

	// ── minimal concrete operator ────────────────────────────────────────────

	private static final class NoOpOperator extends CompositionOperator {

		private final ModelKind kind;

		NoOpOperator(ModelKind kind) {
			this.kind = kind;
		}

		@Override
		protected EquivalenceRelation equivalenceRelation(
				List<JustificationModel> sources, Map<String, String> arguments,
				Map<String, String> knownAliases) {
			return (a, b) -> false;
		}

		@Override
		protected MergeFunction mergeFunction(List<JustificationModel> sources,
				Map<String, String> arguments) {
			return (r, g, a) -> List.of();
		}

		@Override
		protected Command createResultModel(String name,
				SourceLocation location, List<JustificationModel> sources,
				Map<String, String> arguments, List<Command> elements) {
			// The model created is the kind the call produces.
			return kind == ModelKind.TEMPLATE
					? new CreateTemplate(name)
					: new CreateJustification(name);
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
			engine.enrich(unit, new java.util.ArrayList<>(cmds));
			assertThat(unit.findModel("out")).isPresent();
		}

		@Test
		void resultRecordsTheCallThatBuiltIt() {
			// The JPIPE export writes a composed model back as its call.
			Unit unit = unitWithModels("a", "b");
			var op = applyOperator("out", "noop", List.of("b", "a"), Map.of(),
					ModelKind.JUSTIFICATION);
			engine.enrich(unit, new java.util.ArrayList<>(op.expand(unit)));

			assertThat(unit.get("out").composition()).hasValueSatisfying(c -> {
				assertThat(c.operator()).isEqualTo("noop");
				assertThat(c.sources()).containsExactly(unit.get("b"),
						unit.get("a"));
				assertThat(c.arguments()).isEmpty();
				assertThat(c.idArguments()).contains("unifyExclude");
			});
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

	@Nested
	class Namespaces {

		/** A model holding one evidence {@code e}. */
		private static Justification withEvidence(String name, String id) {
			Justification j = new Justification(name);
			j.addElement(new Evidence(id, "Evidence of " + name));
			return j;
		}

		/**
		 * {@code name}, holding the elements of {@code sources} as source:id.
		 */
		private static Justification composed(String name,
				JustificationModel... sources) {
			Justification j = new Justification(name);
			for (JustificationModel source : sources) {
				source.allElements().forEach(e -> j.addElement(new Evidence(
						source.getName() + ":" + e.id(), e.label())));
			}
			j.recordComposition(new Composition("assemble", List.of(sources),
					Map.of(), Set.of()));
			return j;
		}

		@ParameterizedTest(name = "under ns, {0} is {1}")
		@CsvSource({"e, e", "a:e, ns:a:e", "both:a:e, ns:both:ns:a:e",
				"base:t:abs, ns:base:t:abs", "unknown:e, unknown:e"})
		void an_element_id_is_mapped_onto_the_models_that_exist(String id,
				String expected) {
			// A two-stage composition under a namespace names its elements
			// ns:both:ns:a:e: rewriting both:a:e as text gave ns:both:a:e,
			// which names nothing (#178).
			Justification a = withEvidence("ns:a", "e");
			Template t = new Template("ns:base:t");
			t.addElement(new AbstractSupport("abs", "Some support"));
			Justification j = new Justification("ns:j");
			j.inline(t, "ns:base:t");
			List<JustificationModel> sources = List.of(a,
					composed("ns:both", a), j);

			assertThat(ApplyOperator.inUnit(id, "ns", sources))
					.isEqualTo(expected);
		}

		@Test
		void a_segment_names_a_model_only_when_the_rest_is_one_of_its_ids() {
			// Under ns, b:ns:a:e is (b)(ns:a)(e): b:ns holds e, not a:e. The
			// longest model name, b:ns, was read first (#202).
			Justification b = composed("ns:b", withEvidence("ns:ns:a", "e"));
			Justification bNs = withEvidence("ns:b:ns", "e");

			assertThat(ApplyOperator.inUnit("b:ns:a:e", "ns", List.of(b, bNs)))
					.isEqualTo("ns:b:ns:ns:a:e");
		}
	}
}
