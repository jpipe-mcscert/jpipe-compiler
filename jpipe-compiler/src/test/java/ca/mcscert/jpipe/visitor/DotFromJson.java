package ca.mcscert.jpipe.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Rebuilds the DOT export of a model from its JSON export alone, as a consumer
 * such as jpipe-runner would (#199). It shares only the drawing conventions
 * with {@link DotExporter} (label wrapping, node styles, the cluster style):
 * every element, cluster, placeholder and edge comes from the JSON.
 */
public final class DotFromJson {

	private static final String INDENT = DotExporter.INDENT;
	private static final String NL = System.lineSeparator();
	private static final String GHOST = DotExporter.ABSTRACT_SUFFIX;

	private DotFromJson() {
	}

	/**
	 * The DOT text {@link DotExporter} writes for the model of {@code json}.
	 */
	public static String rebuild(String json) {
		JSONObject model = new JSONObject(json);
		Map<String, JSONObject> elements = new LinkedHashMap<>();
		model.getJSONArray("elements").forEach(e -> {
			JSONObject element = (JSONObject) e;
			elements.put(element.getString("id"), element);
		});
		JSONObject layout = model.optJSONObject("implements");

		StringBuilder dot = new StringBuilder();
		String name = model.getString("name");
		dot.append("digraph ").append(DotLabel.quoted(name)).append(" {")
				.append(NL);
		dot.append(INDENT).append("rankdir=BT;").append(NL);
		dot.append(INDENT).append("label=")
				.append(DotLabel
						.wrapAndQuote(display(name, model.getString("type"))))
				.append(";").append(NL);

		List<String> outside = layout == null
				? List.copyOf(elements.keySet())
				: strings(layout.getJSONArray("elements"));
		outside.forEach(id -> node(dot, INDENT, id, elements.get(id)));

		Map<String, Set<String>> routed = new HashMap<>();
		List<String> placeholders = new ArrayList<>();
		if (layout != null) {
			clusters(dot, layout.getJSONArray("templates"), elements, routed,
					placeholders);
		}
		placeholders.forEach(id -> dot.append(INDENT)
				.append(DotLabel.quoted(id)).append(" -> ")
				.append(DotLabel.quoted(id + GHOST))
				.append(" [arrowhead=empty, style=dotted];").append(NL));

		model.getJSONArray("relations").forEach(r -> {
			JSONObject relation = (JSONObject) r;
			String source = relation.getString("source");
			String target = relation.getString("target");
			String from = routed.getOrDefault(source, Set.of()).contains(target)
					? source + GHOST
					: source;
			dot.append(INDENT).append(DotLabel.quoted(from)).append(" -> ")
					.append(DotLabel.quoted(target)).append(";").append(NL);
		});
		dot.append("}").append(NL);
		return dot.toString();
	}

	private static void clusters(StringBuilder dot, JSONArray templates,
			Map<String, JSONObject> elements, Map<String, Set<String>> routed,
			List<String> placeholders) {
		String indent = INDENT;
		for (Object t : templates) {
			JSONObject template = (JSONObject) t;
			Map<String, JSONObject> ghosts = new HashMap<>();
			template.getJSONArray("placeholders").forEach(p -> {
				JSONObject placeholder = (JSONObject) p;
				ghosts.put(placeholder.getString("id"), placeholder);
			});
			dot.append(indent).append("subgraph ")
					.append(DotLabel
							.quoted("cluster_" + template.getString("name")))
					.append(" {").append(NL);
			String inner = indent + INDENT;
			dot.append(inner).append(DotExporter.CLUSTER_ATTRS).append(NL);
			dot.append(inner).append("label=")
					.append(DotLabel.wrapAndQuote(
							display(template.getString("name"), "template")))
					.append(";").append(NL);
			for (String id : strings(template.getJSONArray("elements"))) {
				JSONObject ghost = ghosts.get(id);
				if (ghost == null) {
					node(dot, inner, id, elements.get(id));
				} else {
					line(dot, inner, id + GHOST, ghost.getString("label"),
							DotNodeStyle.ABSTRACT_SUPPORT);
					placeholders.add(id);
					routed.put(id, Set
							.copyOf(strings(ghost.getJSONArray("supports"))));
				}
			}
			indent = inner;
		}
		for (int i = templates.length(); i > 0; i--) {
			dot.append(INDENT.repeat(i)).append("}").append(NL);
		}
	}

	private static void node(StringBuilder dot, String indent, String id,
			JSONObject element) {
		line(dot, indent, id, element.getString("label"),
				styleOf(element.getString("type")));
	}

	private static void line(StringBuilder dot, String indent, String id,
			String label, DotNodeStyle style) {
		dot.append(indent).append(DotLabel.quoted(id)).append(" [label=")
				.append(DotLabel.wrapAndQuote(label)).append(", id=")
				.append(DotLabel.quoted(id)).append(", ")
				.append(style.toAttrs()).append("];").append(NL);
	}

	private static DotNodeStyle styleOf(String type) {
		return switch (type) {
			case "conclusion" -> DotNodeStyle.CONCLUSION;
			case "sub-conclusion" -> DotNodeStyle.SUB_CONCLUSION;
			case "strategy" -> DotNodeStyle.STRATEGY;
			case "evidence" -> DotNodeStyle.EVIDENCE;
			case "abstract-support" -> DotNodeStyle.ABSTRACT_SUPPORT;
			default -> throw new IllegalArgumentException(type);
		};
	}

	private static String display(String name, String type) {
		return "template".equals(type) ? "<<template>> " + name : name;
	}

	private static List<String> strings(JSONArray array) {
		List<String> values = new ArrayList<>();
		array.forEach(v -> values.add((String) v));
		return values;
	}
}
