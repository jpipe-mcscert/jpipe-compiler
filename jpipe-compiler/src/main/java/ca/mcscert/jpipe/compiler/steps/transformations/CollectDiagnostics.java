package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.ExecutedAction;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ActionInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.AliasInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ElementCounts;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ImplementorInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ModelInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.SymbolInfo;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gathers everything a diagnostic report shows into a single
 * {@link DiagnosticSnapshot}.
 *
 * <p>
 * This is the one place that walks the {@link Unit} for reporting purposes. The
 * renderers downstream ({@link DiagnosticReport} for text,
 * {@link JsonDiagnosticReport} for JSON) consume the snapshot and never touch
 * the model, which is what guarantees the two formats describe the same thing.
 */
public final class CollectDiagnostics
		extends
			Transformation<Unit, DiagnosticSnapshot> {

	private static final String KIND_CONCLUSION = "conclusion";
	private static final String KIND_SUB_CONCLUSION = "sub-conclusion";
	private static final String KIND_STRATEGY = "strategy";
	private static final String KIND_EVIDENCE = "evidence";
	private static final String KIND_ABSTRACT_SUPPORT = "abstract-support";

	@Override
	protected DiagnosticSnapshot run(Unit input, CompilationContext ctx) {
		return snapshot(input, ctx);
	}

	/**
	 * Describes what a compilation produced and reported.
	 *
	 * <p>
	 * Callable directly, and not only as a pipeline step, so that a compilation
	 * which aborted on a fatal diagnostic can still be reported on — pass an
	 * empty {@link Unit} for the models it never got to build.
	 *
	 * @param input
	 *            the compiled unit; empty, never {@code null}, when the
	 *            pipeline aborted.
	 * @param ctx
	 *            the context carrying diagnostics, statistics and actions.
	 * @return the render-agnostic report payload.
	 */
	public DiagnosticSnapshot snapshot(Unit input, CompilationContext ctx) {
		Map<String, List<ImplementorInfo>> implementors = implementorsOf(input);
		Map<String, Map<String, String>> aliases = groupAliasesByModel(input);
		List<ModelInfo> models = new ArrayList<>();
		for (JustificationModel<?> model : input.getModels()) {
			models.add(describe(input, model, implementors, aliases));
		}
		return new DiagnosticSnapshot(ctx.sourcePath(), ctx.diagnostics(),
				ctx.stats(), List.copyOf(models), actionsOf(ctx));
	}

	// -------------------------------------------------------------------------
	// Models
	// -------------------------------------------------------------------------

	private ModelInfo describe(Unit unit, JustificationModel<?> model,
			Map<String, List<ImplementorInfo>> implementors,
			Map<String, Map<String, String>> aliasesByModel) {
		String name = model.getName();
		boolean isTemplate = model instanceof Template;
		String parent = model.getParent().map(JustificationModel::getName)
				.orElse(null);
		List<ImplementorInfo> usedBy = isTemplate
				? implementors.getOrDefault(name, List.of())
				: List.of();
		return new ModelInfo(name,
				isTemplate
						? DiagnosticSnapshot.KIND_TEMPLATE
						: DiagnosticSnapshot.KIND_JUSTIFICATION,
				parent, unit.locationOf(name), countElements(model), usedBy,
				collectSymbols(unit, model),
				collectAliases(aliasesByModel.getOrDefault(name, Map.of())));
	}

	/** Maps each template name to the models implementing it. */
	private Map<String, List<ImplementorInfo>> implementorsOf(Unit unit) {
		Map<String, List<ImplementorInfo>> implementors = new LinkedHashMap<>();
		for (JustificationModel<?> model : unit.getModels()) {
			model.getParent()
					.ifPresent(t -> implementors
							.computeIfAbsent(t.getName(),
									k -> new ArrayList<>())
							.add(new ImplementorInfo(model.getName(),
									unit.locationOf(model.getName()))));
		}
		return implementors;
	}

	private ElementCounts countElements(JustificationModel<?> model) {
		return new ElementCounts(model.conclusion().isPresent() ? 1 : 0,
				model.subConclusions().size(), model.strategies().size(),
				model.evidence().size(),
				model.elementsOfType(AbstractSupport.class).size());
	}

	// -------------------------------------------------------------------------
	// Symbols and aliases
	// -------------------------------------------------------------------------

	/**
	 * Collects the model's elements in stable display order: conclusion,
	 * sub-conclusions, strategies, evidence, abstract supports.
	 */
	private List<SymbolInfo> collectSymbols(Unit unit,
			JustificationModel<?> model) {
		List<SymbolInfo> symbols = new ArrayList<>();
		model.conclusion().ifPresent(
				c -> symbols.add(symbol(unit, model, c, KIND_CONCLUSION)));
		addAll(symbols, unit, model, model.subConclusions(),
				KIND_SUB_CONCLUSION);
		addAll(symbols, unit, model, model.strategies(), KIND_STRATEGY);
		addAll(symbols, unit, model, model.evidence(), KIND_EVIDENCE);
		addAll(symbols, unit, model,
				model.elementsOfType(AbstractSupport.class),
				KIND_ABSTRACT_SUPPORT);
		return List.copyOf(symbols);
	}

	private void addAll(List<SymbolInfo> target, Unit unit,
			JustificationModel<?> model,
			List<? extends JustificationElement> elements, String kind) {
		for (JustificationElement element : elements) {
			target.add(symbol(unit, model, element, kind));
		}
	}

	private SymbolInfo symbol(Unit unit, JustificationModel<?> model,
			JustificationElement element, String kind) {
		SourceLocation location = unit.locationOf(model.getName(),
				element.id());
		return new SymbolInfo(element.id(), kind, location);
	}

	private List<AliasInfo> collectAliases(Map<String, String> aliases) {
		return aliases.entrySet().stream()
				.map(e -> new AliasInfo(e.getKey(), e.getValue())).toList();
	}

	/**
	 * Pre-groups the unit's alias map by model name for O(1) lookup per model.
	 *
	 * <p>
	 * Alias keys use the convention {@code "modelName/elementId"}; the slash
	 * separates the model scope from the element id within it.
	 */
	private static Map<String, Map<String, String>> groupAliasesByModel(
			Unit unit) {
		Map<String, Map<String, String>> byModel = new LinkedHashMap<>();
		unit.aliases().forEach((key, newId) -> {
			int slash = key.indexOf('/');
			if (slash >= 0) {
				byModel.computeIfAbsent(key.substring(0, slash),
						k -> new LinkedHashMap<>())
						.put(key.substring(slash + 1), newId);
			}
		});
		return byModel;
	}

	// -------------------------------------------------------------------------
	// Actions
	// -------------------------------------------------------------------------

	private List<ActionInfo> actionsOf(CompilationContext ctx) {
		List<ExecutedAction> actions = ctx.executedActions();
		List<ActionInfo> result = new ArrayList<>(actions.size());
		for (int i = 0; i < actions.size(); i++) {
			ExecutedAction action = actions.get(i);
			result.add(new ActionInfo(i + 1, action.depth(), action.isMacro(),
					String.valueOf(action.command())));
		}
		return List.copyOf(result);
	}
}
