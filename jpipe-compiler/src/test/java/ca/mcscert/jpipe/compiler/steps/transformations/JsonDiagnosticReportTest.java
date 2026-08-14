package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_DEFERRALS;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_MACROS;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_TOTAL;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.ExecutedAction;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.DiagnosticCodes;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Asserts on the parsed document rather than on substrings: the point of the
 * JSON report is its structure, and a substring match would pass on malformed
 * output.
 */
class JsonDiagnosticReportTest {

	private JsonDiagnosticReport step;
	private CompilationContext ctx;
	private Unit unit;

	@BeforeEach
	void setUp() {
		step = new JsonDiagnosticReport();
		ctx = new CompilationContext("test.jd");
		unit = new Unit("test.jd");
	}

	// -------------------------------------------------------------------------
	// Document shape
	// -------------------------------------------------------------------------

	@Nested
	class Document {

		@Test
		void outputIsWellFormedJson() {
			assertThat(report()).isNotNull();
		}

		@Test
		void everySectionOfTheTextReportIsPresent() {
			assertThat(report().keySet()).contains("schemaVersion", "source",
					"status", "diagnostics", "stats", "models", "actions");
		}

		@Test
		void schemaVersionIsDeclared() {
			assertThat(report().getInt("schemaVersion"))
					.isEqualTo(JsonDiagnosticReport.SCHEMA_VERSION);
		}

		@Test
		void sourceIsTheCompiledFile() {
			assertThat(report().getString("source")).isEqualTo("test.jd");
		}

		@Test
		void statusIsOkWhenNothingWasReported() {
			assertThat(report().getString("status")).isEqualTo("ok");
		}

		@Test
		void statusIsErrorsWhenADiagnosticWasReported() {
			ctx.error("something went wrong");
			assertThat(report().getString("status")).isEqualTo("errors");
		}

	}

	// -------------------------------------------------------------------------
	// Diagnostics
	// -------------------------------------------------------------------------

	@Nested
	class Diagnostics {

		@Test
		void codeIsASeparateFieldAndNotInlinedInTheMessage() {
			ctx.error(DiagnosticCodes.UNKNOWN_MODEL, "unknown model 'foo'");

			JSONObject d = report().getJSONArray("diagnostics")
					.getJSONObject(0);

			assertThat(d.getString("code")).isEqualTo("unknown-model");
			assertThat(d.getString("message")).isEqualTo("unknown model 'foo'")
					.doesNotContain("unknown-model");
		}

		@Test
		void codeKeyIsAbsentWhenTheDiagnosticHasNoCode() {
			ctx.error("something went wrong");

			JSONObject d = report().getJSONArray("diagnostics")
					.getJSONObject(0);

			assertThat(d.has("code")).isFalse();
		}

		@Test
		void locationKeysAreAbsentWhenThePositionIsUnknown() {
			ctx.error("something went wrong");

			JSONObject d = report().getJSONArray("diagnostics")
					.getJSONObject(0);

			assertThat(d.has("line")).isFalse();
			assertThat(d.has("column")).isFalse();
		}

		@Test
		void locationIsReportedWhenKnown() {
			ctx.error(DiagnosticCodes.UNKNOWN_ELEMENT, 5, 10, "bad token");

			JSONObject d = report().getJSONArray("diagnostics")
					.getJSONObject(0);

			assertThat(d.getInt("line")).isEqualTo(5);
			assertThat(d.getInt("column")).isEqualTo(10);
			assertThat(d.getString("source")).isEqualTo("test.jd");
		}

		@Test
		void severityIsLowerCased() {
			ctx.error("an error");
			ctx.fatal("a fatal one");

			JSONArray diagnostics = report().getJSONArray("diagnostics");

			assertThat(diagnostics.getJSONObject(0).getString("severity"))
					.isEqualTo("error");
			assertThat(diagnostics.getJSONObject(1).getString("severity"))
					.isEqualTo("fatal");
		}

		@Test
		void diagnosticsKeepReportOrder() {
			ctx.error("first");
			ctx.error("second");

			JSONArray diagnostics = report().getJSONArray("diagnostics");

			assertThat(diagnostics.getJSONObject(0).getString("message"))
					.isEqualTo("first");
			assertThat(diagnostics.getJSONObject(1).getString("message"))
					.isEqualTo("second");
		}
	}

	// -------------------------------------------------------------------------
	// Statistics and actions
	// -------------------------------------------------------------------------

	@Nested
	class StatsAndActions {

		@Test
		void statsAreReadableUnconditionallyEvenWhenNoneWereRecorded() {
			JSONObject stats = report().getJSONObject("stats");

			assertThat(stats.getJSONObject("commands").getInt("total"))
					.isZero();
			assertThat(stats.getInt("deferrals")).isZero();
		}

		@Test
		void recordedStatsAreReported() {
			ctx.recordStat(STAT_COMMANDS_TOTAL, 5L);
			ctx.recordStat(STAT_COMMANDS_MACROS, 2L);
			ctx.recordStat(STAT_COMMANDS_DEFERRALS, 1L);

			JSONObject stats = report().getJSONObject("stats");

			assertThat(stats.getJSONObject("commands").getInt("total"))
					.isEqualTo(5);
			assertThat(stats.getJSONObject("commands").getInt("macros"))
					.isEqualTo(2);
			assertThat(stats.getInt("deferrals")).isEqualTo(1);
		}

		@Test
		void actionsAreEmptyWhenNoneWereRecorded() {
			assertThat(report().getJSONArray("actions")).isEmpty();
		}

		@Test
		void actionsCarryIndexDepthAndMacroFlag() {
			ctx.recordActions(
					List.of(action(new CreateJustification("j1"), 0)));

			JSONObject a = report().getJSONArray("actions").getJSONObject(0);

			assertThat(a.getInt("index")).isEqualTo(1);
			assertThat(a.getInt("depth")).isZero();
			assertThat(a.getBoolean("macro")).isFalse();
			assertThat(a.getString("description"))
					.isEqualTo("create_justification('j1').");
		}
	}

	// -------------------------------------------------------------------------
	// Models and symbols
	// -------------------------------------------------------------------------

	@Nested
	class Models {

		@Test
		void modelsAreEmptyForAnEmptyUnit() {
			assertThat(report().getJSONArray("models")).isEmpty();
		}

		@Test
		void justificationReportsItsKindAndElementCensus() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "A conclusion"));
			j.addElement(new Strategy("s", "A strategy"));
			j.addElement(new Evidence("e", "An evidence"));
			unit.add(j);

			JSONObject model = report().getJSONArray("models").getJSONObject(0);

			assertThat(model.getString("name")).isEqualTo("j");
			assertThat(model.getString("kind")).isEqualTo("justification");
			JSONObject elements = model.getJSONObject("elements");
			assertThat(elements.getInt("conclusion")).isEqualTo(1);
			assertThat(elements.getInt("strategy")).isEqualTo(1);
			assertThat(elements.getInt("evidence")).isEqualTo(1);
			assertThat(elements.getInt("subConclusion")).isZero();
		}

		@Test
		void implementsKeyIsAbsentWhenTheModelImplementsNothing() {
			unit.add(new Justification("j"));

			JSONObject model = report().getJSONArray("models").getJSONObject(0);

			assertThat(model.has("implements")).isFalse();
		}

		@Test
		void templateAndItsImplementorAreLinkedBothWays() {
			Template t = new Template("base");
			unit.add(t);
			Justification j = new Justification("impl");
			j.inline(t, "base");
			unit.add(j);

			JSONArray models = report().getJSONArray("models");
			JSONObject template = byName(models, "base");
			JSONObject impl = byName(models, "impl");

			assertThat(template.getString("kind")).isEqualTo("template");
			assertThat(template.getJSONArray("usedBy").getJSONObject(0)
					.getString("name")).isEqualTo("impl");
			assertThat(impl.getString("implements")).isEqualTo("base");
		}

		@Test
		void symbolCarriesIdKindAndLocation() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "A conclusion"));
			unit.add(j);
			unit.recordLocation("j", new SourceLocation("test.jd", 1, 1));
			unit.recordLocation("j", "c", new SourceLocation("test.jd", 2, 3));

			JSONObject symbol = report().getJSONArray("models").getJSONObject(0)
					.getJSONArray("symbols").getJSONObject(0);

			assertThat(symbol.getString("id")).isEqualTo("c");
			assertThat(symbol.getString("kind")).isEqualTo("conclusion");
			assertThat(symbol.getBoolean("synthesized")).isFalse();
			assertThat(symbol.getJSONObject("location").getInt("line"))
					.isEqualTo(2);
			assertThat(symbol.getJSONObject("location").getInt("column"))
					.isEqualTo(3);
		}

		@Test
		void synthesizedSymbolIsFlaggedAndHasNoLocation() {
			Justification j = new Justification("j");
			j.setConclusion(new Conclusion("c", "A conclusion"));
			unit.add(j);

			JSONObject symbol = report().getJSONArray("models").getJSONObject(0)
					.getJSONArray("symbols").getJSONObject(0);

			assertThat(symbol.getBoolean("synthesized")).isTrue();
			assertThat(symbol.has("location")).isFalse();
		}

		@Test
		void aliasesAreReportedAsFromToPairs() {
			unit.add(new Justification("j"));
			unit.recordAlias("j", "oldId", "newId");

			JSONObject alias = report().getJSONArray("models").getJSONObject(0)
					.getJSONArray("aliases").getJSONObject(0);

			assertThat(alias.getString("from")).isEqualTo("oldId");
			assertThat(alias.getString("to")).isEqualTo("newId");
		}
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private JSONObject report() {
		return new JSONObject(
				step.run(new CollectDiagnostics().run(unit, ctx), ctx));
	}

	private static JSONObject byName(JSONArray models, String name) {
		for (int i = 0; i < models.length(); i++) {
			JSONObject model = models.getJSONObject(i);
			if (name.equals(model.getString("name"))) {
				return model;
			}
		}
		throw new AssertionError("no model named " + name);
	}

	private static ExecutedAction action(Command command, int depth) {
		return new ExecutedAction(command, depth);
	}
}
