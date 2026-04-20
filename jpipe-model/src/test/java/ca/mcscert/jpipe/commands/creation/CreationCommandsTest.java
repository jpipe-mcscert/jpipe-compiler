package ca.mcscert.jpipe.commands.creation;

import static org.assertj.core.api.Assertions.assertThat;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class CreationCommandsTest {

	// -------------------------------------------------------------------------
	// RegularCommand contract
	// -------------------------------------------------------------------------

	@Nested
	class RegularCommandContract {

		private final RegularCommand cmd = new RegularCommand() {
			@Override
			public void doExecute(Unit context) {
				// intentionally empty
			}
		};

		@Test
		void defaultConditionIsAlwaysTrue() {
			assertThat(cmd.condition().test(new Unit("x"))).isTrue();
		}
	}

	// -------------------------------------------------------------------------
	// Unit-level creation commands
	// -------------------------------------------------------------------------

	@Nested
	class CreateJustificationTest {

		@Test
		void addsJustificationToUnit() {
			Unit unit = new Unit("src");
			new CreateJustification("j1").execute(unit);
			assertThat(unit.findModel("j1")).isPresent().get()
					.isInstanceOf(Justification.class);
		}

		@Test
		void registersLocationWhenProvided() {
			Unit unit = new Unit("src");
			SourceLocation loc = new SourceLocation(2, 0);
			new CreateJustification("j1", loc).execute(unit);
			assertThat(unit.locationOf("j1")).isEqualTo(loc);
		}

		@Test
		void locationIsUnknownWhenNotProvided() {
			Unit unit = new Unit("src");
			new CreateJustification("j1").execute(unit);
			assertThat(unit.locationOf("j1")).isEqualTo(SourceLocation.UNKNOWN);
		}

		@Test
		void toStringIncludesIdentifier() {
			assertThat(new CreateJustification("j1").toString()).contains("j1");
		}
	}

	@Nested
	class CreateTemplateTest {

		@Test
		void addsTemplateToUnit() {
			Unit unit = new Unit("src");
			new CreateTemplate("t1").execute(unit);
			assertThat(unit.findModel("t1")).isPresent().get()
					.isInstanceOf(Template.class);
		}

		@Test
		void registersLocationWhenProvided() {
			Unit unit = new Unit("src");
			SourceLocation loc = new SourceLocation(5, 0);
			new CreateTemplate("t1", loc).execute(unit);
			assertThat(unit.locationOf("t1")).isEqualTo(loc);
		}

		@Test
		void locationIsUnknownWhenNotProvided() {
			Unit unit = new Unit("src");
			new CreateTemplate("t1").execute(unit);
			assertThat(unit.locationOf("t1")).isEqualTo(SourceLocation.UNKNOWN);
		}

		@Test
		void toStringIncludesIdentifier() {
			assertThat(new CreateTemplate("t1").toString()).contains("t1");
		}
	}

	// -------------------------------------------------------------------------
	// Element-level creation commands (require a container to pre-exist)
	// -------------------------------------------------------------------------

	@Nested
	class CreateConclusionTest {

		@Test
		void setsConclusionOnModel() {
			Unit unit = unitWithJustification("j1");
			new CreateConclusion("j1", "c1", "my conclusion").execute(unit);
			assertThat(unit.get("j1").conclusion()).isPresent().get()
					.extracting(Conclusion::id).isEqualTo("c1");
		}

		@Test
		void registersLocationWhenProvided() {
			Unit unit = unitWithJustification("j1");
			SourceLocation loc = new SourceLocation(3, 2);
			new CreateConclusion("j1", "c1", "my conclusion", loc)
					.execute(unit);
			assertThat(unit.locationOf("j1", "c1")).isEqualTo(loc);
		}

		@Test
		void locationIsUnknownWhenNotProvided() {
			Unit unit = unitWithJustification("j1");
			new CreateConclusion("j1", "c1", "my conclusion").execute(unit);
			assertThat(unit.locationOf("j1", "c1"))
					.isEqualTo(SourceLocation.UNKNOWN);
		}

		@Test
		void toStringIncludesFields() {
			String s = new CreateConclusion("j1", "c1", "lbl").toString();
			assertThat(s).contains("j1").contains("c1").contains("lbl");
		}
	}

	@Nested
	class CreateStrategyTest {

		@Test
		void addsStrategyToContainer() {
			Unit unit = unitWithJustification("j1");
			new CreateStrategy("j1", "s1", "my strategy").execute(unit);
			assertThat(unit.get("j1").getElements()).hasSize(1).first()
					.isInstanceOf(Strategy.class)
					.extracting(e -> ((Strategy) e).id()).isEqualTo("s1");
		}

		@Test
		void registersLocationWhenProvided() {
			Unit unit = unitWithJustification("j1");
			SourceLocation loc = new SourceLocation(4, 2);
			new CreateStrategy("j1", "s1", "my strategy", loc).execute(unit);
			assertThat(unit.locationOf("j1", "s1")).isEqualTo(loc);
		}

		@Test
		void locationIsUnknownWhenNotProvided() {
			Unit unit = unitWithJustification("j1");
			new CreateStrategy("j1", "s1", "my strategy").execute(unit);
			assertThat(unit.locationOf("j1", "s1"))
					.isEqualTo(SourceLocation.UNKNOWN);
		}

		@Test
		void toStringIncludesFields() {
			String s = new CreateStrategy("j1", "s1", "lbl").toString();
			assertThat(s).contains("j1").contains("s1").contains("lbl");
		}
	}

	@Nested
	class CreateEvidenceTest {

		@Test
		void addsEvidenceToContainer() {
			Unit unit = unitWithJustification("j1");
			new CreateEvidence("j1", "e1", "my evidence").execute(unit);
			assertThat(unit.get("j1").getElements()).hasSize(1).first()
					.isInstanceOf(Evidence.class)
					.extracting(e -> ((Evidence) e).id()).isEqualTo("e1");
		}

		@Test
		void registersLocationWhenProvided() {
			Unit unit = unitWithJustification("j1");
			SourceLocation loc = new SourceLocation(7, 2);
			new CreateEvidence("j1", "e1", "my evidence", loc).execute(unit);
			assertThat(unit.locationOf("j1", "e1")).isEqualTo(loc);
		}

		@Test
		void locationIsUnknownWhenNotProvided() {
			Unit unit = unitWithJustification("j1");
			new CreateEvidence("j1", "e1", "my evidence").execute(unit);
			assertThat(unit.locationOf("j1", "e1"))
					.isEqualTo(SourceLocation.UNKNOWN);
		}

		@Test
		void toStringIncludesFields() {
			String s = new CreateEvidence("j1", "e1", "lbl").toString();
			assertThat(s).contains("j1").contains("e1").contains("lbl");
		}
	}

	@Nested
	class CreateSubConclusionTest {

		@Test
		void addsSubConclusionToContainer() {
			Unit unit = unitWithJustification("j1");
			new CreateSubConclusion("j1", "sc1", "my sub-conclusion")
					.execute(unit);
			assertThat(unit.get("j1").getElements()).hasSize(1).first()
					.isInstanceOf(SubConclusion.class)
					.extracting(e -> ((SubConclusion) e).id()).isEqualTo("sc1");
		}

		@Test
		void registersLocationWhenProvided() {
			Unit unit = unitWithJustification("j1");
			SourceLocation loc = new SourceLocation(6, 2);
			new CreateSubConclusion("j1", "sc1", "my sub-conclusion", loc)
					.execute(unit);
			assertThat(unit.locationOf("j1", "sc1")).isEqualTo(loc);
		}

		@Test
		void locationIsUnknownWhenNotProvided() {
			Unit unit = unitWithJustification("j1");
			new CreateSubConclusion("j1", "sc1", "my sub-conclusion")
					.execute(unit);
			assertThat(unit.locationOf("j1", "sc1"))
					.isEqualTo(SourceLocation.UNKNOWN);
		}

		@Test
		void toStringIncludesFields() {
			String s = new CreateSubConclusion("j1", "sc1", "lbl").toString();
			assertThat(s).contains("j1").contains("sc1").contains("lbl");
		}
	}

	@Nested
	class CreateAbstractSupportTest {

		@Test
		void addsAbstractSupportToContainer() {
			Unit unit = unitWithTemplate("t1");
			new CreateAbstractSupport("t1", "as1", "my abstract support")
					.execute(unit);
			assertThat(unit.get("t1").getElements()).hasSize(1).first()
					.isInstanceOf(AbstractSupport.class)
					.extracting(e -> ((AbstractSupport) e).id())
					.isEqualTo("as1");
		}

		@Test
		void registersLocationWhenProvided() {
			Unit unit = unitWithTemplate("t1");
			SourceLocation loc = new SourceLocation(8, 2);
			new CreateAbstractSupport("t1", "as1", "my abstract support", loc)
					.execute(unit);
			assertThat(unit.locationOf("t1", "as1")).isEqualTo(loc);
		}

		@Test
		void locationIsUnknownWhenNotProvided() {
			Unit unit = unitWithTemplate("t1");
			new CreateAbstractSupport("t1", "as1", "my abstract support")
					.execute(unit);
			assertThat(unit.locationOf("t1", "as1"))
					.isEqualTo(SourceLocation.UNKNOWN);
		}

		@Test
		void toStringIncludesFields() {
			String s = new CreateAbstractSupport("t1", "as1", "lbl").toString();
			assertThat(s).contains("t1").contains("as1").contains("lbl");
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private static Unit unitWithJustification(String name) {
		Unit unit = new Unit("src");
		unit.add(new Justification(name));
		return unit;
	}

	private static Unit unitWithTemplate(String name) {
		Unit unit = new Unit("src");
		unit.add(new Template(name));
		return unit;
	}
}
