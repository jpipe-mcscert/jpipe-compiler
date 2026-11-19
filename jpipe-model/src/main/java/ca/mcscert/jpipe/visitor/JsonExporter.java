package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.List;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Serialises a single {@link JustificationModel} to JSON text.
 *
 * <p>
 * Uses {@code org.json} (already a declared dependency) for correct string
 * escaping. The library is used internally as a builder; the pipeline type
 * remains {@link String}, consistent with other text exporters (ADR-0013).
 *
 * <p>
 * Output schema:
 *
 * <pre>{@code
 * {
 *   "name": "...",
 *   "type": "justification" | "template",
 *   "elements": [ { "type": "...", "id": "...", "label": "...",
 *                   "aliases": [ "..." ],
 *                   "origins": [ { "model": "...", "id": "...",
 *                                  "type": "..." }, ... ] }, ... ],
 *   "relations": [ { "source": "...", "target": "..." }, ... ]
 * }
 * }</pre>
 *
 * <p>
 * The optional per-element {@code "aliases"} array lists the qualified original
 * ids that were merged into the element during composition/unification; it is
 * omitted for elements that are not the target of any alias. Merges compose, so
 * the array is transitive: composing the result of a composition records a
 * chain, and the array lists every id along it, down to those written in the
 * original source models.
 *
 * <p>
 * The optional per-element {@code "origins"} array is written for the elements
 * of a model built by an operator: the elements of the models it was composed
 * from that the element was built from, each with the name of its model, its id
 * and its kind as the JSON export of that model writes them. It covers every
 * level of composition, nearest first, so that an element whose kind
 * composition changed (the hook {@code refine} merges into a sub-conclusion,
 * the conclusion of a source {@code assemble} takes) says what it was. It is
 * omitted for an element the operator created itself, and for every element of
 * a model no operator built (#200).
 */
public class JsonExporter extends AbstractModelExporter {

	private JSONObject result;
	private JSONArray elements;
	private JSONArray relations;
	private Map<String, List<CompositionOrigins.Origin>> origins;

	/**
	 * Serialise {@code model} to JSON text.
	 *
	 * @param model
	 *            the justification or template to serialise.
	 * @return JSON string.
	 */
	public String export(JustificationModel model) {
		result = null;
		model.accept(this);
		return result.toString(2);
	}

	// -------------------------------------------------------------------------
	// Element visit methods
	// -------------------------------------------------------------------------

	@Override
	public Void visit(Conclusion conclusion) {
		appendElement("conclusion", conclusion);
		return null;
	}

	@Override
	public Void visit(SubConclusion subConclusion) {
		appendElement("sub-conclusion", subConclusion);
		return null;
	}

	@Override
	public Void visit(Strategy strategy) {
		appendElement("strategy", strategy);
		return null;
	}

	@Override
	public Void visit(Evidence evidence) {
		appendElement("evidence", evidence);
		return null;
	}

	@Override
	public Void visit(AbstractSupport abstractSupport) {
		appendElement("abstract-support", abstractSupport);
		return null;
	}

	// -------------------------------------------------------------------------
	// AbstractModelExporter
	// -------------------------------------------------------------------------

	@Override
	protected void exportModel(JustificationModel model) {
		String type = model instanceof Justification
				? "justification"
				: "template";
		currentModelName = model.getName();
		initAliases(model);
		origins = CompositionOrigins.of(model);
		elements = new JSONArray();
		relations = new JSONArray();

		model.allElements().forEach(e -> e.accept(this));
		model.forEachSupportEdge((supported, supporter) -> appendRelation(
				qualify(supporter), qualify(supported)));

		result = new JSONObject();
		result.put("name", model.getName());
		result.put("type", type);
		result.put("elements", elements);
		result.put("relations", relations);
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private void appendElement(String type, JustificationElement element) {
		JSONObject obj = new JSONObject();
		obj.put("type", type);
		obj.put("id", qualify(element.id()));
		obj.put("label", element.label());
		List<String> aliases = qualifiedOriginalsOf(element.id());
		if (!aliases.isEmpty()) {
			obj.put("aliases", new JSONArray(aliases));
		}
		List<CompositionOrigins.Origin> from = origins.get(element.id());
		if (from != null) {
			JSONArray array = new JSONArray();
			from.forEach(o -> array.put(new JSONObject().put("model", o.model())
					.put("id", o.id()).put("type", o.type())));
			obj.put("origins", array);
		}
		elements.put(obj);
	}

	private void appendRelation(String source, String target) {
		JSONObject obj = new JSONObject();
		obj.put("source", source);
		obj.put("target", target);
		relations.put(obj);
	}
}
