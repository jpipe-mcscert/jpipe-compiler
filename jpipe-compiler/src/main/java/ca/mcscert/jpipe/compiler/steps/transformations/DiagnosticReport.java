package ca.mcscert.jpipe.compiler.steps.transformations;

import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_DEFERRALS;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_MACROS;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_TOTAL;
import ca.mcscert.jpipe.compiler.model.Diagnostic;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ActionInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.AliasInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ElementCounts;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ModelInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.SymbolInfo;
import ca.mcscert.jpipe.model.SourceLocation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Renders a {@link DiagnosticSnapshot} as the human-readable report printed by
 * the {@code diagnostic} CLI command.
 *
 * <p>
 * The report has five sections, in this order:
 * <ol>
 * <li><b>Diagnostics</b> — errors from compilation.</li>
 * <li><b>Action Statistics</b> — command counts and deferral count.</li>
 * <li><b>Model Summary</b> — per model: type, parent, element counts.</li>
 * <li><b>Symbol Table</b> — full location registry.</li>
 * <li><b>Executed Actions</b> — the model-construction command trace.</li>
 * </ol>
 *
 * <p>
 * Sections whose underlying data is absent (no statistics recorded, no actions
 * executed) are omitted entirely rather than printed empty.
 *
 * @see CollectDiagnostics
 * @see JsonDiagnosticReport
 */
public final class DiagnosticReport extends DiagnosticRenderer {

	private static final String HDR_DIAGNOSTICS = "=== Diagnostics ===\n";
	private static final String HDR_ACTION_STATS = "\n=== Action Statistics ===\n";
	private static final String HDR_MODEL_SUMMARY = "\n=== Model Summary ===\n";
	private static final String HDR_EXECUTED_ACTIONS = "\n=== Executed Actions ===\n";
	private static final String HDR_SYMBOL_TABLE = "\n=== Symbol Table ===\n";
	private static final String INDENT = "  ";
	private static final String ARROW = "\u2192";

	@Override
	public String render(DiagnosticSnapshot input) {
		StringBuilder sb = new StringBuilder();
		appendDiagnostics(sb, input);
		appendActionStats(sb, input);
		appendModelSummary(sb, input);
		appendSymbolTable(sb, input);
		appendActionList(sb, input);
		return sb.toString();
	}

	private void appendDiagnostics(StringBuilder sb, DiagnosticSnapshot input) {
		sb.append(HDR_DIAGNOSTICS);
		if (input.diagnostics().isEmpty()) {
			sb.append("(none)\n");
		} else {
			for (Diagnostic d : input.diagnostics()) {
				String message = d.hasCode()
						? "[" + d.code() + "] " + d.message()
						: d.message();
				if (d.hasLocation()) {
					sb.append(String.format("[%s] %s:%d:%d: %s%n", d.level(),
							d.source(), d.line(), d.column(), message));
				} else {
					sb.append(String.format("[%s] %s: %s%n", d.level(),
							d.source(), message));
				}
			}
		}
	}

	private void appendActionStats(StringBuilder sb, DiagnosticSnapshot input) {
		Map<String, Long> stats = input.stats();
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

	private void appendModelSummary(StringBuilder sb,
			DiagnosticSnapshot input) {
		sb.append(HDR_MODEL_SUMMARY);
		boolean first = true;
		for (ModelInfo model : input.models()) {
			if (!first) {
				sb.append("\n");
			}
			first = false;
			String parent = model.implementsTemplate()
					? "  [implements \"" + model.implementedTemplate() + "\"]"
					: "";
			sb.append(String.format("%s \"%s\"%s%n", model.kind(), model.name(),
					parent));
			sb.append(String.format("  elements:  %s%n",
					elementSummary(model.counts())));
			appendTemplateImplementors(sb, model);
		}
	}

	private void appendTemplateImplementors(StringBuilder sb, ModelInfo model) {
		if (model.usedBy().isEmpty()) {
			return;
		}
		String names = model.usedBy().stream().map(impl -> {
			SourceLocation loc = impl.location();
			return "\"" + impl.name() + "\""
					+ (loc.isKnown()
							? " @ " + loc.line() + ":" + loc.column()
							: "");
		}).collect(Collectors.joining(", "));
		sb.append(String.format("  used by:   %s%n", names));
	}

	private String elementSummary(ElementCounts counts) {
		List<String> parts = new ArrayList<>();
		count(parts, "conclusion", counts.conclusion());
		count(parts, "sub-conclusion", counts.subConclusion());
		count(parts, "strategy", counts.strategy());
		count(parts, "evidence", counts.evidence());
		count(parts, "abstract-support", counts.abstractSupport());
		return parts.isEmpty() ? "(empty)" : String.join(", ", parts);
	}

	private void count(List<String> parts, String label, int n) {
		if (n > 0) {
			parts.add(label + "(" + n + ")");
		}
	}

	private void appendActionList(StringBuilder sb, DiagnosticSnapshot input) {
		if (input.actions().isEmpty()) {
			return;
		}
		sb.append(HDR_EXECUTED_ACTIONS);
		for (ActionInfo action : input.actions()) {
			String indent = INDENT.repeat(action.depth());
			String label = action.macro() ? "[macro] " : "";
			sb.append(String.format("%3d. %s%s%s%n", action.index(), indent,
					label, action.description()));
		}
	}

	private void appendSymbolTable(StringBuilder sb, DiagnosticSnapshot input) {
		sb.append(HDR_SYMBOL_TABLE);
		if (input.models().isEmpty()) {
			sb.append("(empty)\n");
			return;
		}
		for (ModelInfo model : input.models()) {
			appendModelSymbolEntry(sb, model);
		}
	}

	private void appendModelSymbolEntry(StringBuilder sb, ModelInfo model) {
		sb.append(String.format("%s \"%s\"  [%s]%n", model.kind(), model.name(),
				model.location()));
		appendElementEntries(sb, model);
		appendAliasEntries(sb, model.aliases());
	}

	private void appendElementEntries(StringBuilder sb, ModelInfo model) {
		List<SymbolInfo> symbols = model.symbols();
		if (symbols.isEmpty()) {
			return;
		}
		int maxLen = symbols.stream().mapToInt(s -> s.id().length()).max()
				.orElse(0);
		String modelSource = model.location().source();
		for (SymbolInfo symbol : symbols) {
			sb.append(String.format("  %s  %s%n", padEnd(symbol.id(), maxLen),
					locationOf(symbol, modelSource)));
		}
	}

	/**
	 * Renders an element's location relative to its model: bare
	 * {@code line:column} when the element sits in the same file as its model,
	 * fully qualified when it was pulled in from another one.
	 */
	private String locationOf(SymbolInfo symbol, String modelSource) {
		if (symbol.isSynthesized()) {
			return "[synthesized]";
		}
		SourceLocation loc = symbol.location();
		boolean sameFile = modelSource != null
				&& modelSource.equals(loc.source());
		return sameFile ? loc.line() + ":" + loc.column() : loc.toString();
	}

	private void appendAliasEntries(StringBuilder sb, List<AliasInfo> aliases) {
		if (aliases.isEmpty()) {
			return;
		}
		int maxLen = aliases.stream().mapToInt(a -> a.from().length()).max()
				.orElse(0);
		for (AliasInfo alias : aliases) {
			sb.append(String.format("  %s  %s %s  [alias]%n",
					padEnd(alias.from(), maxLen), ARROW, alias.to()));
		}
	}

	private static String padEnd(String text, int width) {
		StringBuilder sb = new StringBuilder(text);
		while (sb.length() < width) {
			sb.append(' ');
		}
		return sb.toString();
	}
}
