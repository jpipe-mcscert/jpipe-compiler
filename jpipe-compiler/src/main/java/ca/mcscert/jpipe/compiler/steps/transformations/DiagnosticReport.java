package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.ExecutedAction;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_DEFERRALS;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_MACROS;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_TOTAL;
import ca.mcscert.jpipe.compiler.model.Diagnostic;
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
import java.util.stream.Collectors;

/**
 * Produces a human-readable diagnostic report from the compiled {@link Unit}
 * and the {@link CompilationContext}.
 *
 * <p>
 * The report has four sections:
 * <ol>
 * <li><b>Diagnostics</b> — errors from compilation.</li>
 * <li><b>Action Statistics</b> — command counts and deferral count.</li>
 * <li><b>Model Summary</b> — per model: type, parent, element counts.</li>
 * <li><b>Symbol Table</b> — full location registry.</li>
 * </ol>
 */
public final class DiagnosticReport extends Transformation<Unit, String> {

	private static final String HDR_DIAGNOSTICS = "=== Diagnostics ===\n";
	private static final String HDR_ACTION_STATS = "\n=== Action Statistics ===\n";
	private static final String HDR_MODEL_SUMMARY = "\n=== Model Summary ===\n";
	private static final String HDR_EXECUTED_ACTIONS = "\n=== Executed Actions ===\n";
	private static final String HDR_SYMBOL_TABLE = "\n=== Symbol Table ===\n";
	private static final String INDENT = "  ";
	private static final String ARROW = "\u2192";

	@Override
	protected String run(Unit input, CompilationContext ctx) {
		StringBuilder sb = new StringBuilder();
		appendDiagnostics(sb, ctx);
		appendActionStats(sb, ctx);
		appendModelSummary(sb, input);
		appendSymbolTable(sb, input);
		appendActionList(sb, ctx);
		ctx.markDiagnosticsRendered();
		return sb.toString();
	}

	private void appendDiagnostics(StringBuilder sb, CompilationContext ctx) {
		sb.append(HDR_DIAGNOSTICS);
		if (ctx.diagnostics().isEmpty()) {
			sb.append("(none)\n");
		} else {
			for (Diagnostic d : ctx.diagnostics()) {
				if (d.hasLocation()) {
					sb.append(String.format("[%s] %s:%d:%d: %s%n", d.level(),
							d.source(), d.line(), d.column(), d.message()));
				} else {
					sb.append(String.format("[%s] %s: %s%n", d.level(),
							d.source(), d.message()));
				}
			}
		}
	}

	private void appendActionStats(StringBuilder sb, CompilationContext ctx) {
		Map<String, Long> stats = ctx.stats();
		if (stats.isEmpty()) {
			return;
		}
		sb.append(HDR_ACTION_STATS);
		long total = stats.getOrDefault(STAT_COMMANDS_TOTAL, 0L);
		long macros = stats.getOrDefault(STAT_COMMANDS_MACROS, 0L);
		long deferrals = stats.getOrDefault(STAT_COMMANDS_DEFERRALS, 0L);
		sb.append(String.format("commands: %d total (%d macro)%n", total,
				macros));
		sb.append(String.format("deferrals: %d%n", deferrals));
	}

	private void appendModelSummary(StringBuilder sb, Unit unit) {
		sb.append(HDR_MODEL_SUMMARY);
		Map<String, List<String>> implementors = new LinkedHashMap<>();
		for (JustificationModel<?> model : unit.getModels()) {
			model.getParent()
					.ifPresent(t -> implementors
							.computeIfAbsent(t.getName(),
									k -> new ArrayList<>())
							.add(model.getName()));
		}
		boolean first = true;
		for (JustificationModel<?> model : unit.getModels()) {
			if (!first) {
				sb.append("\n");
			}
			first = false;
			String kind = model instanceof Template
					? "template"
					: "justification";
			String parent = model.getParent()
					.map(t -> "  [implements \"" + t.getName() + "\"]")
					.orElse("");
			sb.append(String.format("%s \"%s\"%s%n", kind, model.getName(),
					parent));
			sb.append(
					String.format("  elements:  %s%n", elementSummary(model)));
			if (model instanceof Template) {
				List<String> impls = implementors.getOrDefault(model.getName(),
						List.of());
				if (!impls.isEmpty()) {
					String names = impls.stream().map(n -> {
						SourceLocation loc = unit.locationOf(n);
						return "\"" + n + "\""
								+ (loc.isKnown()
										? " @ " + loc.line() + ":"
												+ loc.column()
										: "");
					}).collect(Collectors.joining(", "));
					sb.append(String.format("  used by:   %s%n", names));
				}
			}
		}
	}

	private String elementSummary(JustificationModel<?> model) {
		List<String> parts = new ArrayList<>();
		model.conclusion().ifPresent(c -> parts.add("conclusion(1)"));
		count(parts, "sub-conclusion", model.subConclusions().size());
		count(parts, "strategy", model.strategies().size());
		count(parts, "evidence", model.evidence().size());
		count(parts, "abstract-support",
				model.elementsOfType(AbstractSupport.class).size());
		return parts.isEmpty() ? "(empty)" : String.join(", ", parts);
	}

	private void count(List<String> parts, String label, int n) {
		if (n > 0) {
			parts.add(label + "(" + n + ")");
		}
	}

	private void appendActionList(StringBuilder sb, CompilationContext ctx) {
		List<ExecutedAction> actions = ctx.executedActions();
		if (actions.isEmpty()) {
			return;
		}
		sb.append(HDR_EXECUTED_ACTIONS);
		for (int i = 0; i < actions.size(); i++) {
			ExecutedAction action = actions.get(i);
			String indent = INDENT.repeat(action.depth());
			String label = action.isMacro() ? "[macro] " : "";
			sb.append(String.format("%3d. %s%s%s%n", i + 1, indent, label,
					action.command()));
		}
	}

	private void appendSymbolTable(StringBuilder sb, Unit unit) {
		sb.append(HDR_SYMBOL_TABLE);
		if (unit.getModels().isEmpty()) {
			sb.append("(empty)\n");
			return;
		}

		// Pre-group aliases by model name for O(1) lookup per model.
		Map<String, Map<String, String>> aliasesByModel = new LinkedHashMap<>();
		unit.aliases().forEach((key, newId) -> {
			int slash = key.indexOf('/');
			if (slash >= 0) {
				aliasesByModel
						.computeIfAbsent(key.substring(0, slash),
								k -> new LinkedHashMap<>())
						.put(key.substring(slash + 1), newId);
			}
		});

		for (JustificationModel<?> model : unit.getModels()) {
			String modelName = model.getName();
			SourceLocation modelLoc = unit.locationOf(modelName);
			String kind = (model instanceof Template)
					? "template"
					: "justification";
			sb.append(String.format("%s \"%s\"  [%s]%n", kind, modelName,
					modelLoc));

			// Collect all elements in a stable order.
			List<JustificationElement> elements = new ArrayList<>();
			model.conclusion().ifPresent(elements::add);
			elements.addAll(model.subConclusions());
			elements.addAll(model.strategies());
			elements.addAll(model.evidence());
			elements.addAll(model.elementsOfType(AbstractSupport.class));

			if (!elements.isEmpty()) {
				int maxLen = elements.stream().mapToInt(e -> e.id().length())
						.max().orElse(0);
				String modelSource = modelLoc.source();
				for (JustificationElement e : elements) {
					SourceLocation elemLoc = unit.locationOf(modelName, e.id());
					String locStr;
					if (elemLoc.isKnown()) {
						locStr = (modelSource != null
								&& modelSource.equals(elemLoc.source()))
										? elemLoc.line() + ":"
												+ elemLoc.column()
										: elemLoc.toString();
					} else {
						locStr = "[synthesized]";
					}
					sb.append(String.format("  %-" + maxLen + "s  %s%n", e.id(),
							locStr));
				}
			}

			Map<String, String> modelAliases = aliasesByModel
					.getOrDefault(modelName, Map.of());
			if (!modelAliases.isEmpty()) {
				int maxLen = modelAliases.keySet().stream()
						.mapToInt(String::length).max().orElse(0);
				modelAliases
						.forEach((oldId,
								newId) -> sb.append(String.format(
										"  %-" + maxLen + "s  " + ARROW
												+ " %s  [alias]%n",
										oldId, newId)));
			}
		}
	}
}
