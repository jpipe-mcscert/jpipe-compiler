package ca.mcscert.jpipe.compiler.steps.transformations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_TOTAL;

import ca.mcscert.jpipe.commands.ExecutedAction;
import ca.mcscert.jpipe.commands.creation.CreateJustification;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ModelInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.SymbolInfo;
import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers the model traversal that both report renderers depend on. Everything
 * asserted here is what keeps the text and JSON reports describing the same
 * compilation.
 */
class CollectDiagnosticsTest {

	private CollectDiagnostics step;
	private CompilationContext ctx;
	private Unit unit;

	@BeforeEach
	void setUp() {
		step = new CollectDiagnostics();
		ctx = new CompilationContext("test.jd");
		unit = new Unit("test.jd");
	}

	// -------------------------------------------------------------------------
	// Context pass-through
	// -------------------------------------------------------------------------

	@Test
	void carriesTheSourcePathFromTheContext() {
		assertThat(collect().source()).isEqualTo("test.jd");
	}

	@Test
	void carriesDiagnosticsInReportOrder() {
		ctx.error("first");
		ctx.error("second");

		assertThat(collect().diagnostics()).extracting(d -> d.message())
				.containsExactly("first", "second");
	}

	@Test
	void hasErrorsMirrorsTheContext() {
		assertThat(collect().hasErrors()).isFalse();
		ctx.error("boom");
		assertThat(collect().hasErrors()).isTrue();
	}

	@Test
	void carriesRecordedStats() {
		ctx.recordStat(STAT_COMMANDS_TOTAL, 7L);

		assertThat(collect().stats()).containsEntry(STAT_COMMANDS_TOTAL, 7L);
	}

	@Test
	void numbersActionsFromOne() {
		ctx.recordActions(
				List.of(new ExecutedAction(new CreateJustification("j1"), 0),
						new ExecutedAction(new CreateJustification("j2"), 1)));

		assertThat(collect().actions()).extracting(a -> a.index())
				.containsExactly(1, 2);
		assertThat(collect().actions().get(1).depth()).isEqualTo(1);
	}

	/**
	 * The context hands out unmodifiable <em>views</em> of live state, so a
	 * snapshot that kept those references would keep changing after collection
	 * and two renderings of it could disagree.
	 */
	@Test
	void snapshotIsUnaffectedByLaterChangesToTheContext() {
		ctx.error("reported before collection");
		DiagnosticSnapshot snapshot = collect();

		ctx.error("reported after collection");
		ctx.recordStat(STAT_COMMANDS_TOTAL, 99L);
		ctx.recordActions(List
				.of(new ExecutedAction(new CreateJustification("late"), 0)));

		assertThat(snapshot.diagnostics()).hasSize(1);
		assertThat(snapshot.stats()).isEmpty();
		assertThat(snapshot.actions()).isEmpty();
	}

	@Test
	void snapshotCollectionsRejectModification() {
		unit.add(new Justification("j"));
		ctx.error("boom");
		DiagnosticSnapshot snapshot = collect();

		assertThatThrownBy(() -> snapshot.diagnostics().clear())
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> snapshot.models().clear())
				.isInstanceOf(UnsupportedOperationException.class);
		assertThatThrownBy(() -> snapshot.models().get(0).symbols().clear())
				.isInstanceOf(UnsupportedOperationException.class);
	}

	@Test
	void collectingDoesNotMarkDiagnosticsRendered() {
		collect();

		// Only a renderer may claim the diagnostics were shown to the user;
		// otherwise a failure between collect and render would silently
		// swallow ChainCompiler's fallback dump.
		assertThat(ctx.diagnosticsRendered()).isFalse();
	}

	// -------------------------------------------------------------------------
	// Models
	// -------------------------------------------------------------------------

	@Test
	void distinguishesJustificationsFromTemplates() {
		unit.add(new Justification("j"));
		unit.add(new Template("t"));

		assertThat(collect().models()).extracting(ModelInfo::kind)
				.containsExactly("justification", "template");
	}

	@Test
	void countsElementsByType() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "A conclusion"));
		j.addElement(new SubConclusion("sc", "A sub-conclusion"));
		j.addElement(new Strategy("s", "A strategy"));
		j.addElement(new Evidence("e", "An evidence"));
		unit.add(j);

		var counts = collect().models().get(0).counts();

		assertThat(counts.conclusion()).isEqualTo(1);
		assertThat(counts.subConclusion()).isEqualTo(1);
		assertThat(counts.strategy()).isEqualTo(1);
		assertThat(counts.evidence()).isEqualTo(1);
		assertThat(counts.abstractSupport()).isZero();
		assertThat(counts.isEmpty()).isFalse();
	}

	@Test
	void emptyModelHasEmptyCounts() {
		unit.add(new Justification("j"));

		assertThat(collect().models().get(0).counts().isEmpty()).isTrue();
	}

	@Test
	void linksATemplateToItsImplementors() {
		Template t = new Template("base");
		unit.add(t);
		Justification j = new Justification("impl");
		j.inline(t, "base");
		unit.add(j);

		ModelInfo template = modelNamed("base");
		ModelInfo impl = modelNamed("impl");

		assertThat(template.isTemplate()).isTrue();
		assertThat(template.usedBy()).extracting(u -> u.name())
				.containsExactly("impl");
		assertThat(impl.implementsTemplate()).isTrue();
		assertThat(impl.implementedTemplate()).isEqualTo("base");
	}

	@Test
	void justificationNeverReportsImplementors() {
		unit.add(new Justification("j"));

		assertThat(modelNamed("j").usedBy()).isEmpty();
	}

	// -------------------------------------------------------------------------
	// Symbols and aliases
	// -------------------------------------------------------------------------

	@Test
	void collectsSymbolsInStableDisplayOrder() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "A conclusion"));
		j.addElement(new SubConclusion("sc", "A sub-conclusion"));
		j.addElement(new Strategy("s", "A strategy"));
		j.addElement(new Evidence("e", "An evidence"));
		unit.add(j);

		assertThat(modelNamed("j").symbols()).extracting(SymbolInfo::kind)
				.containsExactly("conclusion", "sub-conclusion", "strategy",
						"evidence");
	}

	@Test
	void resolvesElementLocations() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "A conclusion"));
		unit.add(j);
		unit.recordLocation("j", new SourceLocation("test.jd", 1, 1));
		unit.recordLocation("j", "c", new SourceLocation("test.jd", 2, 3));

		SymbolInfo symbol = modelNamed("j").symbols().get(0);

		assertThat(symbol.isSynthesized()).isFalse();
		assertThat(symbol.location().line()).isEqualTo(2);
		assertThat(symbol.location().column()).isEqualTo(3);
	}

	@Test
	void flagsElementsWithoutALocationAsSynthesized() {
		Justification j = new Justification("j");
		j.setConclusion(new Conclusion("c", "A conclusion"));
		unit.add(j);

		assertThat(modelNamed("j").symbols().get(0).isSynthesized()).isTrue();
	}

	@Test
	void groupsAliasesUnderTheirOwningModel() {
		unit.add(new Justification("j"));
		unit.add(new Justification("other"));
		unit.recordAlias("j", "oldId", "newId");

		assertThat(modelNamed("j").aliases()).extracting(a -> a.from())
				.containsExactly("oldId");
		assertThat(modelNamed("other").aliases()).isEmpty();
	}

	// -------------------------------------------------------------------------
	// Helpers
	// -------------------------------------------------------------------------

	private DiagnosticSnapshot collect() {
		return step.run(unit, ctx);
	}

	private ModelInfo modelNamed(String name) {
		return collect().models().stream().filter(m -> name.equals(m.name()))
				.findFirst()
				.orElseThrow(() -> new AssertionError("no model " + name));
	}
}
