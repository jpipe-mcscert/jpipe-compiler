package ca.mcscert.jpipe.compiler.model;

import ca.mcscert.jpipe.model.SourceLocation;
import java.util.List;
import java.util.Map;

/**
 * An immutable, render-agnostic view of everything a diagnostic report shows.
 *
 * <p>
 * Produced once by
 * {@code ca.mcscert.jpipe.compiler.steps.transformations.CollectDiagnostics}
 * from a compiled {@link ca.mcscert.jpipe.model.Unit} and its
 * {@link CompilationContext}, then consumed by every report renderer. Holding
 * the collection step in common is what keeps the human-readable and JSON
 * reports from drifting apart: a section added here surfaces in both, and
 * neither renderer walks the model itself.
 *
 * <p>
 * This record carries <em>data only</em> — no formatting. Presentation choices
 * (bracketing codes, padding columns, eliding unknown locations) belong to the
 * renderers.
 *
 * @param source
 *            path of the compiled source file.
 * @param diagnostics
 *            all diagnostics reported during compilation, in report order.
 * @param stats
 *            named compilation statistics in insertion order; empty when the
 *            interpretation step did not run.
 * @param models
 *            one entry per model in the unit, in declaration order.
 * @param actions
 *            the ordered list of executed actions; empty when not recorded.
 */
public record DiagnosticSnapshot(String source, List<Diagnostic> diagnostics,
		Map<String, Long> stats, List<ModelInfo> models,
		List<ActionInfo> actions) {

	/** Model kind discriminator: a concrete justification. */
	public static final String KIND_JUSTIFICATION = "justification";

	/** Model kind discriminator: a reusable template. */
	public static final String KIND_TEMPLATE = "template";

	/** True if any ERROR or FATAL diagnostic was reported. */
	public boolean hasErrors() {
		return diagnostics.stream().anyMatch(Diagnostic::isError);
	}

	/**
	 * A single model: its identity, its element census, and its symbols.
	 *
	 * @param name
	 *            the model's name.
	 * @param kind
	 *            {@link #KIND_JUSTIFICATION} or {@link #KIND_TEMPLATE}.
	 * @param implementedTemplate
	 *            name of the template this model implements, or {@code null}.
	 * @param location
	 *            where the model is declared.
	 * @param counts
	 *            how many elements of each type the model holds.
	 * @param usedBy
	 *            models implementing this one; always empty for a
	 *            justification.
	 * @param symbols
	 *            the model's elements, in stable display order.
	 * @param aliases
	 *            element ids rewritten during composition.
	 */
	public record ModelInfo(String name, String kind,
			String implementedTemplate, SourceLocation location,
			ElementCounts counts, List<ImplementorInfo> usedBy,
			List<SymbolInfo> symbols, List<AliasInfo> aliases) {

		/** True if this model implements a template. */
		public boolean implementsTemplate() {
			return implementedTemplate != null;
		}

		/** True if this model is a template rather than a justification. */
		public boolean isTemplate() {
			return KIND_TEMPLATE.equals(kind);
		}
	}

	/**
	 * How many elements of each type a model holds. {@code conclusion} is 0 or
	 * 1: the model owns at most one.
	 *
	 * @param conclusion
	 *            1 when the model has a conclusion, 0 otherwise.
	 * @param subConclusion
	 *            number of sub-conclusions.
	 * @param strategy
	 *            number of strategies.
	 * @param evidence
	 *            number of evidence elements.
	 * @param abstractSupport
	 *            number of abstract support placeholders.
	 */
	public record ElementCounts(int conclusion, int subConclusion, int strategy,
			int evidence, int abstractSupport) {

		/** True when the model holds no elements at all. */
		public boolean isEmpty() {
			return conclusion + subConclusion + strategy + evidence
					+ abstractSupport == 0;
		}
	}

	/**
	 * A model that implements the template being described.
	 *
	 * @param name
	 *            the implementing model's name.
	 * @param location
	 *            where the implementing model is declared.
	 */
	public record ImplementorInfo(String name, SourceLocation location) {
	}

	/**
	 * One entry of a model's symbol table.
	 *
	 * @param id
	 *            the element's id within its model.
	 * @param kind
	 *            element type, kebab-case (e.g. {@code "sub-conclusion"}).
	 * @param location
	 *            where the element is declared, or
	 *            {@link SourceLocation#UNKNOWN} when it was synthesized during
	 *            template expansion or composition.
	 */
	public record SymbolInfo(String id, String kind, SourceLocation location) {

		/** True when the element has no source location of its own. */
		public boolean isSynthesized() {
			return !location.isKnown();
		}
	}

	/**
	 * An element id rewritten during composition.
	 *
	 * @param from
	 *            the original id.
	 * @param to
	 *            the id it now resolves to.
	 */
	public record AliasInfo(String from, String to) {
	}

	/**
	 * One executed model-construction action.
	 *
	 * @param index
	 *            1-based position in the execution order.
	 * @param depth
	 *            nesting depth inside macro expansions.
	 * @param macro
	 *            true when the action is a macro command.
	 * @param description
	 *            the command's own rendering of itself.
	 */
	public record ActionInfo(int index, int depth, boolean macro,
			String description) {
	}
}
