package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.commands.ExecutedAction;
import ca.mcscert.jpipe.compiler.model.CompilationContext;
import ca.mcscert.jpipe.compiler.model.Diagnostic;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
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

	@Override
	protected String run(Unit input, CompilationContext ctx) {
		StringBuilder sb = new StringBuilder();
		appendDiagnostics(sb, ctx);
		appendActionStats(sb, ctx);
		appendModelSummary(sb, input);
		appendSymbolTable(sb, input);
		appendActionList(sb, ctx);
		return sb.toString();
	}

	private void appendDiagnostics(StringBuilder sb, CompilationContext ctx) {
		sb.append("=== Diagnostics ===\n");
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
		sb.append("\n=== Action Statistics ===\n");
		long total = stats.getOrDefault("commands.total", 0L);
		long macros = stats.getOrDefault("commands.macros", 0L);
		long deferrals = stats.getOrDefault("commands.deferrals", 0L);
		sb.append(String.format("commands: %d total (%d macro)%n", total,
				macros));
		sb.append(String.format("deferrals: %d%n", deferrals));
	}

	private void appendModelSummary(StringBuilder sb, Unit unit) {
		sb.append("\n=== Model Summary ===\n");
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
		sb.append("\n=== Executed Actions ===\n");
		for (int i = 0; i < actions.size(); i++) {
			ExecutedAction action = actions.get(i);
			String indent = "  ".repeat(action.depth());
			String label = action.isMacro() ? "[macro] " : "";
			sb.append(String.format("%3d. %s%s%s%n", i + 1, indent, label,
					action.command()));
		}
	}

	private void appendSymbolTable(StringBuilder sb, Unit unit) {
		sb.append("\n=== Symbol Table ===\n");
		Map<String, SourceLocation> locs = unit.locations();
		if (locs.isEmpty()) {
			sb.append("(empty)\n");
			return;
		}

		// Group element entries by model name, preserving insertion order.
		// Keys are either "model" (model-level) or "model/element"
		// (element-level).
		Map<String, Map<String, SourceLocation>> byModel = new LinkedHashMap<>();
		locs.forEach((key, loc) -> {
			int slash = key.indexOf('/');
			if (slash < 0) {
				byModel.computeIfAbsent(key, k -> new LinkedHashMap<>());
			} else {
				String model = key.substring(0, slash);
				String element = key.substring(slash + 1);
				byModel.computeIfAbsent(model, k -> new LinkedHashMap<>())
						.put(element, loc);
			}
		});

		byModel.forEach((modelName, elements) -> {
			SourceLocation modelLoc = locs.getOrDefault(modelName,
					SourceLocation.UNKNOWN);
			JustificationModel<?> model = unit.findModel(modelName)
					.orElse(null);
			String kind = (model instanceof Template)
					? "template"
					: "justification";
			sb.append(String.format("%s \"%s\"  [%s]%n", kind, modelName,
					modelLoc));

			if (!elements.isEmpty()) {
				int maxLen = elements.keySet().stream().mapToInt(String::length)
						.max().orElse(0);
				String modelSource = modelLoc.source();
				elements.forEach((elemId, elemLoc) -> {
					String locStr = (modelSource != null
							&& modelSource.equals(elemLoc.source()))
									? elemLoc.line() + ":" + elemLoc.column()
									: elemLoc.toString();
					sb.append(String.format("  %-" + maxLen + "s  %s%n", elemId,
							locStr));
				});
			}
		});
	}
}
