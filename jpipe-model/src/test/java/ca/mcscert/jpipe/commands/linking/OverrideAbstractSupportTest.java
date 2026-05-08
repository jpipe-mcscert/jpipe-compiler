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
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.List;
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
