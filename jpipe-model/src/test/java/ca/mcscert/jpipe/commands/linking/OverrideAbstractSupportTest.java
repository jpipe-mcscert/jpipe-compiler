package ca.mcscert.jpipe.commands.linking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ca.mcscert.jpipe.commands.ExecutionEngine;
import ca.mcscert.jpipe.commands.creation.CreateAbstractSupport;
import ca.mcscert.jpipe.commands.creation.CreateConclusion;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.commands.creation.CreateStrategy;
import ca.mcscert.jpipe.commands.creation.CreateTemplate;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OverrideAbstractSupportTest {

	private ExecutionEngine engine;

	@BeforeEach
	void setUp() {
		engine = new ExecutionEngine();
	}

	/**
	 * Builds a unit with template t (tc←ts←as) and justification j implements
	 * t.
	 */
	private Unit unitWithAbstractSupport() {
		return engine.spawn("src", List.of(new CreateTemplate("t"),
				new CreateConclusion("t", "tc", "Template conclusion"),
				new CreateStrategy("t", "ts", "Template strategy"),
				new CreateAbstractSupport("t", "as", "Placeholder"),
				new AddSupport("t", "ts", "as"),
				new AddSupport("t", "tc", "ts"), new CreateJustification("j"),
				new CreateConclusion("j", "c", "My conclusion"),
				new ImplementsTemplate("j", "t")));
	}

	// ── accessors ────────────────────────────────────────────────────────────

	@Nested
	class Accessors {

		@Test
		void container() {
			var cmd = new OverrideAbstractSupport("j", "t:as", "evidence",
					"label");
			assertThat(cmd.container()).isEqualTo("j");
		}

		@Test
		void qualifiedId() {
			var cmd = new OverrideAbstractSupport("j", "t:as", "evidence",
					"label");
			assertThat(cmd.qualifiedId()).isEqualTo("t:as");
		}

		@Test
		void newType() {
			var cmd = new OverrideAbstractSupport("j", "t:as", "evidence",
					"label");
			assertThat(cmd.newType()).isEqualTo("evidence");
		}

		@Test
		void label() {
			var cmd = new OverrideAbstractSupport("j", "t:as", "evidence",
					"my label");
			assertThat(cmd.label()).isEqualTo("my label");
		}

		@Test
		void locationDefaultsToUnknown() {
			var cmd = new OverrideAbstractSupport("j", "t:as", "evidence",
					"label");
			assertThat(cmd.location()).isEqualTo(SourceLocation.UNKNOWN);
		}

		@Test
		void toStringContainsKeyFields() {
			var cmd = new OverrideAbstractSupport("j", "t:as", "evidence",
					"label");
			assertThat(cmd)
					.hasToString("override('j', 't:as', evidence, 'label')");
		}
	}

	// ── condition ────────────────────────────────────────────────────────────

	@Nested
	class Condition {

		@Test
		void falseWhenModelAbsent() {
			Unit unit = engine.spawn("src", List.of());
			var cmd = new OverrideAbstractSupport("missing", "t:as", "evidence",
					"label");
			assertThat(cmd.condition().test(unit)).isFalse();
		}

		@Test
		void falseWhenAbstractSupportNotYetPresent() {
			Unit unit = engine.spawn("src",
					List.of(new CreateJustification("j"),
							new CreateConclusion("j", "c", "C")));
			var cmd = new OverrideAbstractSupport("j", "t:as", "evidence",
					"label");
			assertThat(cmd.condition().test(unit)).isFalse();
		}
	}

	// ── expand ───────────────────────────────────────────────────────────────

	@Nested
	class Expand {

		@Test
		void replacesAbstractSupportWithSubConclusion() {
			Unit unit = unitWithAbstractSupport();
			engine.enrich(unit, List.of(new OverrideAbstractSupport("j", "t:as",
					"sub-conclusion", "Refined")));

			Justification j = (Justification) unit.get("j");
			assertThat(j.findById("t:as")).isPresent().get()
					.isInstanceOf(SubConclusion.class);
		}

		@Test
		void rewiresEveryStrategyThePlaceholderSupports() {
			Unit unit = engine.spawn("src",
					List.of(new CreateTemplate("t"),
							new CreateConclusion("t", "tc",
									"Template conclusion"),
							new CreateStrategy("t", "ts1", "First strategy"),
							new CreateStrategy("t", "ts2", "Second strategy"),
							new CreateAbstractSupport("t", "as", "Placeholder"),
							new AddSupport("t", "ts1", "as"),
							new AddSupport("t", "ts2", "as"),
							new AddSupport("t", "tc", "ts1"),
							new CreateJustification("j"),
							new CreateConclusion("j", "c", "My conclusion"),
							new ImplementsTemplate("j", "t")));
			engine.enrich(unit, List.of(new OverrideAbstractSupport("j", "t:as",
					"evidence", "Concrete")));

			Justification j = (Justification) unit.get("j");
			Evidence concrete = (Evidence) j.findById("t:as").orElseThrow();
			assertThat(Stream.of("t:ts1", "t:ts2")
					.map(id -> (Strategy) j.findById(id).orElseThrow())
					.map(Strategy::getSupports)).hasSize(2)
					.allSatisfy(supports -> assertThat(supports)
							.containsExactly(concrete));
		}

		@Test
		void replacesAPlaceholderThatSupportsNoStrategy() {
			// That is a mistake of the template, reported there once; it used
			// to fail every override as well (#183).
			Unit unit = engine.spawn("src",
					List.of(new CreateTemplate("t"),
							new CreateConclusion("t", "tc",
									"Template conclusion"),
							new CreateAbstractSupport("t", "as", "Unused"),
							new CreateJustification("j"),
							new ImplementsTemplate("j", "t")));
			engine.enrich(unit, List.of(new OverrideAbstractSupport("j", "t:as",
					"evidence", "Concrete")));

			assertThat(unit.get("j").findById("t:as")).get()
					.isInstanceOf(Evidence.class);
		}

		@Test
		void anIdThatNamesTwoPlaceholdersIsAmbiguous() {
			// A composition keeps each source's copy of a @support: 't:as'
			// names both, and the element order used to pick one (#177).
			Unit unit = new Unit("src");
			Justification j = new Justification("j");
			j.addElement(new AbstractSupport("l:t:as", "Left"));
			j.addElement(new AbstractSupport("r:t:as", "Right"));
			unit.add(j);
			var cmd = new OverrideAbstractSupport("j", "t:as", "evidence",
					"Which one?");
			assertThatThrownBy(() -> cmd.expand(unit))
					.isInstanceOf(AmbiguousIdException.class)
					.hasMessageContaining("'l:t:as', 'r:t:as'");
		}

		@Test
		void unknownTypeThrowsIllegalArgumentException() {
			Unit unit = unitWithAbstractSupport();
			var cmd = new OverrideAbstractSupport("j", "t:as", "strategy",
					"label");
			assertThatThrownBy(() -> cmd.expand(unit))
					.isInstanceOf(IllegalArgumentException.class)
					.hasMessageContaining("strategy");
		}
	}
}
