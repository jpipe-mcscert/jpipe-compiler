package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.compiler.model.CompilationContext;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_DEFERRALS;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_MACROS;
import static ca.mcscert.jpipe.compiler.model.CompilationContext.STAT_COMMANDS_TOTAL;
import ca.mcscert.jpipe.compiler.model.Diagnostic;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ActionInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.AliasInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ElementCounts;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ImplementorInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.ModelInfo;
import ca.mcscert.jpipe.compiler.model.DiagnosticSnapshot.SymbolInfo;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.SourceLocation;
import java.util.Map;
import java.util.Optional;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Renders a {@link DiagnosticSnapshot} as JSON, for tools that consume the
 * diagnostic report programmatically (IDE integrations, CI dashboards).
 *
 * <p>
 * The document mirrors the human-readable report section for section:
 *
 * <pre>
 * {
 *   "schemaVersion": 1,
 *   "source": "examples/foo.jd",
 *   "status": "ok" | "errors",
 *   "diagnostics": [ { "severity", "code"?, "source", "line"?, "column"?, "message" } ],
 *   "stats": { "commands": { "total", "macros" }, "deferrals" },
 *   "models": [ { "name", "kind", "implements"?, "location",
 *                 "elements": { … }, "usedBy": [ … ],
 *                 "symbols": [ … ], "aliases": [ … ] } ],
 *   "actions": [ { "index", "depth", "macro", "description" } ]
 * }
 * </pre>
 *
 * <p>
 * Optional keys are <em>omitted</em> rather than emitted as {@code null}: a
 * diagnostic without a code has no {@code code} key, and one without a source
 * location has neither {@code line} nor {@code column}. {@code schemaVersion}
 * lets consumers detect the shape; later additions stay additive.
 *
 * <p>
 * Uses {@code org.json} internally as a builder for correct string escaping;
 * the pipeline type remains {@link String}, consistent with the other text
 * emitters (ADR-0013).
 *
 * @see CollectDiagnostics
 * @see DiagnosticReport
 */
public final class JsonDiagnosticReport
		extends
			Transformation<DiagnosticSnapshot, String> {

	/** Version of the emitted document shape. */
	public static final int SCHEMA_VERSION = 1;

	private static final int INDENT = 2;
	private static final String KEY_LINE = "line";
	private static final String KEY_COLUMN = "column";
	private static final String KEY_SOURCE = "source";
	private static final String KEY_NAME = "name";

	@Override
	protected String run(DiagnosticSnapshot input, CompilationContext ctx) {
		JSONObject result = new JSONObject();
		result.put("schemaVersion", SCHEMA_VERSION);
		result.put(KEY_SOURCE, input.source());
		result.put("status", input.hasErrors() ? "errors" : "ok");
		result.put("diagnostics", diagnostics(input));
		result.put("stats", stats(input.stats()));
		result.put("models", models(input));
		result.put("actions", actions(input));
		ctx.markDiagnosticsRendered();
		return result.toString(INDENT);
	}

	// -------------------------------------------------------------------------
	// Sections
	// -------------------------------------------------------------------------

	private JSONArray diagnostics(DiagnosticSnapshot input) {
		JSONArray array = new JSONArray();
		for (Diagnostic d : input.diagnostics()) {
			JSONObject entry = new JSONObject();
			entry.put("severity", d.level().name().toLowerCase());
			if (d.hasCode()) {
				entry.put("code", d.code());
			}
			entry.put(KEY_SOURCE, d.source());
			if (d.hasLocation()) {
				entry.put(KEY_LINE, d.line());
				entry.put(KEY_COLUMN, d.column());
			}
			entry.put("message", d.message());
			array.put(entry);
		}
		return array;
	}

	/**
	 * Statistics are reported as an object of zero values when the
	 * interpretation step did not run, so consumers can read the keys
	 * unconditionally.
	 */
	private JSONObject stats(Map<String, Long> stats) {
		JSONObject commands = new JSONObject();
		commands.put("total", stats.getOrDefault(STAT_COMMANDS_TOTAL, 0L));
		commands.put("macros", stats.getOrDefault(STAT_COMMANDS_MACROS, 0L));
		JSONObject result = new JSONObject();
		result.put("commands", commands);
		result.put("deferrals",
				stats.getOrDefault(STAT_COMMANDS_DEFERRALS, 0L));
		return result;
	}

	private JSONArray models(DiagnosticSnapshot input) {
		JSONArray array = new JSONArray();
		for (ModelInfo model : input.models()) {
			JSONObject entry = new JSONObject();
			entry.put(KEY_NAME, model.name());
			entry.put("kind", model.kind());
			if (model.implementsTemplate()) {
				entry.put("implements", model.implementedTemplate());
			}
			location(model.location())
					.ifPresent(loc -> entry.put("location", loc));
			entry.put("elements", elements(model.counts()));
			entry.put("usedBy", usedBy(model));
			entry.put("symbols", symbols(model));
			entry.put("aliases", aliases(model));
			array.put(entry);
		}
		return array;
	}

	private JSONObject elements(ElementCounts counts) {
		JSONObject entry = new JSONObject();
		entry.put("conclusion", counts.conclusion());
		entry.put("subConclusion", counts.subConclusion());
		entry.put("strategy", counts.strategy());
		entry.put("evidence", counts.evidence());
		entry.put("abstractSupport", counts.abstractSupport());
		return entry;
	}

	private JSONArray usedBy(ModelInfo model) {
		JSONArray array = new JSONArray();
		for (ImplementorInfo impl : model.usedBy()) {
			JSONObject entry = new JSONObject();
			entry.put(KEY_NAME, impl.name());
			location(impl.location())
					.ifPresent(loc -> entry.put("location", loc));
			array.put(entry);
		}
		return array;
	}

	private JSONArray symbols(ModelInfo model) {
		JSONArray array = new JSONArray();
		for (SymbolInfo symbol : model.symbols()) {
			JSONObject entry = new JSONObject();
			entry.put("id", symbol.id());
			entry.put("kind", symbol.kind());
			entry.put("synthesized", symbol.isSynthesized());
			location(symbol.location())
					.ifPresent(loc -> entry.put("location", loc));
			array.put(entry);
		}
		return array;
	}

	private JSONArray aliases(ModelInfo model) {
		JSONArray array = new JSONArray();
		for (AliasInfo alias : model.aliases()) {
			JSONObject entry = new JSONObject();
			entry.put("from", alias.from());
			entry.put("to", alias.to());
			array.put(entry);
		}
		return array;
	}

	private JSONArray actions(DiagnosticSnapshot input) {
		JSONArray array = new JSONArray();
		for (ActionInfo action : input.actions()) {
			JSONObject entry = new JSONObject();
			entry.put("index", action.index());
			entry.put("depth", action.depth());
			entry.put("macro", action.macro());
			entry.put("description", action.description());
			array.put(entry);
		}
		return array;
	}

	// -------------------------------------------------------------------------
	// Utility
	// -------------------------------------------------------------------------

	/**
	 * A location object, or empty when the position is unknown — the key is
	 * then omitted rather than emitted as null.
	 */
	private Optional<JSONObject> location(SourceLocation loc) {
		if (loc == null || !loc.isKnown()) {
			return Optional.empty();
		}
		JSONObject entry = new JSONObject();
		if (loc.source() != null) {
			entry.put(KEY_SOURCE, loc.source());
		}
		entry.put(KEY_LINE, loc.line());
		entry.put(KEY_COLUMN, loc.column());
		return Optional.of(entry);
	}
}
