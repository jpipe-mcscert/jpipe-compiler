package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.util.LabelEscaper;
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
 *   "elements": [ { "type": "...", "id": "...", "label": "..." }, ... ],
 *   "relations": [ { "source": "...", "target": "..." }, ... ]
 * }
 * }</pre>
 */
public class JsonExporter implements JustificationVisitor<Void> {

	private String currentModelName;
	private JSONObject result;
	private JSONArray elements;
	private JSONArray relations;

	/**
	 * Serialise {@code model} to JSON text.
	 *
	 * @param model
	 *            the justification or template to serialise.
	 * @return JSON string.
	 */
	public String export(JustificationModel<?> model) {
		result = null;
		model.accept(this);
		return result.toString(2);
	}

	// -------------------------------------------------------------------------
	// Unit and model visit methods
	// -------------------------------------------------------------------------

	@Override
	public Void visit(Unit unit) {
		throw new UnsupportedOperationException(
				"JsonExporter operates on a single model"
						+ " — use SelectModel to extract one from a Unit");
	}

	@Override
	public Void visit(Justification justification) {
		exportModelBody(justification, "justification");
		return null;
	}

	@Override
	public Void visit(Template template) {
		exportModelBody(template, "template");
		return null;
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
	// Private helpers
	// -------------------------------------------------------------------------

	private void exportModelBody(JustificationModel<?> model, String type) {
		currentModelName = model.getName();
		elements = new JSONArray();
		relations = new JSONArray();

		model.conclusion().ifPresent(c -> c.accept(this));
		model.getElements().forEach(e -> e.accept(this));
		exportRelations(model);

		result = new JSONObject();
		result.put("name", model.getName());
		result.put("type", type);
		result.put("elements", elements);
		result.put("relations", relations);
	}

	private void exportRelations(JustificationModel<?> model) {
		model.conclusion().ifPresent(c -> c.getSupport().ifPresent(
				s -> appendRelation(qualify(s.id()), qualify(c.id()))));
		model.subConclusions().forEach(sc -> sc.getSupport().ifPresent(
				s -> appendRelation(qualify(s.id()), qualify(sc.id()))));
		model.strategies()
				.forEach(s -> s.getSupport()
						.ifPresent(leaf -> appendRelation(
								qualify(((JustificationElement) leaf).id()),
								qualify(s.id()))));
	}

	private void appendElement(String type, JustificationElement element) {
		JSONObject obj = new JSONObject();
		obj.put("type", type);
		obj.put("id", qualify(element.id()));
		obj.put("label", element.label());
		obj.put("escaped", LabelEscaper.toMethodName(element.label()));
		elements.put(obj);
	}

	private void appendRelation(String source, String target) {
		JSONObject obj = new JSONObject();
		obj.put("source", source);
		obj.put("target", target);
		relations.put(obj);
	}

	private String qualify(String elementId) {
		return currentModelName + ":" + elementId;
	}
}
