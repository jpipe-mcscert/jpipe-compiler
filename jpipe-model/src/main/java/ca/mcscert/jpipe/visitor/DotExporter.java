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
import java.util.ArrayList;
import java.util.List;

/**
 * Serialises a single {@link JustificationModel} to Graphviz DOT text, ready to
 * be compiled by {@code dot}. Both {@link Justification} and {@link Template}
 * produce a valid {@code digraph}; {@code AbstractSupport} nodes in templates
 * are rendered as dotted rectangles.
 *
 * <p>
 * This visitor operates on one model at a time. Use {@code SelectModel} in the
 * compilation pipeline to extract the desired model from a {@link Unit} before
 * invoking this exporter.
 *
 * <p>
 * Each node is emitted with shape and colour attributes that mirror the visual
 * conventions of the original jPipe toolchain.
 */
public class DotExporter implements JustificationVisitor<Void> {

	private static final String INDENT = "  ";
	private static final int WRAP_WIDTH = 40;

	// -------------------------------------------------------------------------
	// Node visual styles — change shapes and colours here
	// -------------------------------------------------------------------------

	/**
	 * Visual properties of a single node type. {@code dotStyle} is the DOT
	 * {@code style=} value (e.g. {@code "filled"} or {@code "filled,rounded"});
	 * {@code fillColor} maps to {@code fillcolor=}; {@code borderColor} maps to
	 * {@code color=}. Any field may be {@code null} to omit that attribute. Hex
	 * color values (e.g. {@code "#0072B2"}) are quoted automatically.
	 */
	private record NodeStyle(String shape, String dotStyle, String fillColor,
			String borderColor) {

		/** Renders this style as a DOT attribute fragment. */
		String toAttrs() {
			StringBuilder sb = new StringBuilder("shape=").append(shape);
			if (dotStyle != null) {
				sb.append(", style=");
				if (dotStyle.contains(","))
					sb.append('"').append(dotStyle).append('"');
				else
					sb.append(dotStyle);
			}
			if (fillColor != null)
				sb.append(", fillcolor=").append(dotColor(fillColor));
			if (borderColor != null)
				sb.append(", color=").append(dotColor(borderColor));
			return sb.toString();
		}

		/** Wraps hex color values in quotes; leaves X11 named colors as-is. */
		private static String dotColor(String color) {
			return color.startsWith("#") ? "\"" + color + "\"" : color;
		}
	}

	// Palette derived from Okabe-Ito (2008) — safe for deuteranopia,
	// protanopia, and tritanopia. Fills use lightened versions of the
	// base hues so that black text remains readable (WCAG AA).
	private static final NodeStyle CONCLUSION_STYLE = new NodeStyle("rect",
			"filled,rounded", "lightgrey", null);
	private static final NodeStyle SUB_CONCLUSION_STYLE = new NodeStyle("rect",
			null, null, "#0072B2"); // Okabe-Ito blue
	private static final NodeStyle STRATEGY_STYLE = new NodeStyle("hexagon",
			"filled", "#F0C27F", null); // amber — replaces green
	private static final NodeStyle EVIDENCE_STYLE = new NodeStyle("note",
			"filled", "#9ECAE1", null); // sky blue
	private static final NodeStyle ABSTRACT_SUPPORT_STYLE = new NodeStyle(
			"rect", "dotted", null, null);

	// -------------------------------------------------------------------------

	private final StringBuilder builder = new StringBuilder();
	private String currentModelName;

	/**
	 * Serialise {@code model} to DOT text.
	 *
	 * @param model
	 *            the justification or template to serialise.
	 * @return DOT source text ready to be piped to {@code dot}.
	 */
	public String export(JustificationModel<?> model) {
		builder.setLength(0);
		model.accept(this);
		return builder.toString();
	}

	// ---------------------------------------------------------------------------
	// Unit and model visit methods
	// ---------------------------------------------------------------------------

	@Override
	public Void visit(Unit unit) {
		throw new UnsupportedOperationException(
				"DotExporter operates on a single model — use SelectModel to extract one from a Unit");
	}

	@Override
	public Void visit(Justification justification) {
		exportModelBody(justification);
		return null;
	}

	@Override
	public Void visit(Template template) {
		exportModelBody(template);
		return null;
	}

	// ---------------------------------------------------------------------------
	// Element visit methods
	// ---------------------------------------------------------------------------

	@Override
	public Void visit(Conclusion conclusion) {
		appendNode(conclusion, CONCLUSION_STYLE.toAttrs());
		return null;
	}

	@Override
	public Void visit(SubConclusion subConclusion) {
		appendNode(subConclusion, SUB_CONCLUSION_STYLE.toAttrs());
		return null;
	}

	@Override
	public Void visit(Strategy strategy) {
		appendNode(strategy, STRATEGY_STYLE.toAttrs());
		return null;
	}

	@Override
	public Void visit(Evidence evidence) {
		appendNode(evidence, EVIDENCE_STYLE.toAttrs());
		return null;
	}

	@Override
	public Void visit(AbstractSupport abstractSupport) {
		appendNode(abstractSupport, ABSTRACT_SUPPORT_STYLE.toAttrs());
		return null;
	}

	// ---------------------------------------------------------------------------
	// Private helpers
	// ---------------------------------------------------------------------------

	private void exportModelBody(JustificationModel<?> model) {
		currentModelName = model.getName();
		builder.append("digraph ").append(quoted(model.getName())).append(" {")
				.append(System.lineSeparator());
		builder.append(INDENT).append("rankdir=BT;")
				.append(System.lineSeparator());
		builder.append(INDENT).append("label=")
				.append(wrapAndQuoteLabel(model.getName())).append(";")
				.append(System.lineSeparator());
		model.conclusion().ifPresent(c -> c.accept(this));
		model.getElements().forEach(e -> e.accept(this));
		exportEdges(model);
		builder.append("}").append(System.lineSeparator());
	}

	private void exportEdges(JustificationModel<?> model) {
		model.conclusion().ifPresent(c -> c.getSupport()
				.ifPresent(s -> appendEdge(qualify(s.id()), qualify(c.id()))));
		model.subConclusions().forEach(sc -> sc.getSupport()
				.ifPresent(s -> appendEdge(qualify(s.id()), qualify(sc.id()))));
		model.strategies()
				.forEach(s -> s.getSupport()
						.ifPresent(leaf -> appendEdge(
								qualify(((JustificationElement) leaf).id()),
								qualify(s.id()))));
	}

	private void appendNode(JustificationElement element, String attrs) {
		String qid = qualify(element.id());
		builder.append(INDENT).append(quoted(qid)).append(" [label=")
				.append(wrapAndQuoteLabel(element.label())).append(", id=")
				.append(quoted(qid)).append(", ").append(attrs).append("];")
				.append(System.lineSeparator());
	}

	private void appendEdge(String fromId, String toId) {
		builder.append(INDENT).append(quoted(fromId)).append(" -> ")
				.append(quoted(toId)).append(";")
				.append(System.lineSeparator());
	}

	private String qualify(String elementId) {
		return currentModelName + ":" + elementId;
	}

	private static String quoted(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	/**
	 * Wraps {@code label} at word boundaries so that no line exceeds
	 * {@link #WRAP_WIDTH} characters, then returns the result as a DOT-quoted
	 * string. Lines are separated by {@code \n}, which Graphviz renders as a
	 * centred newline inside a node label.
	 *
	 * <p>
	 * Each word is escaped for backslashes and double-quotes before measuring
	 * and joining, so the wrap decision is based on the characters that will
	 * actually appear in the rendered label.
	 */
	private static String wrapAndQuoteLabel(String label) {
		String[] words = label.trim().split("\\s+");
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : words) {
			String escaped = word.replace("\\", "\\\\").replace("\"", "\\\"")
					.replace("_", "\\_");
			if (current.length() > 0
					&& current.length() + 1 + escaped.length() > WRAP_WIDTH) {
				lines.add(current.toString());
				current = new StringBuilder(escaped);
			} else {
				if (current.length() > 0)
					current.append(' ');
				current.append(escaped);
			}
		}
		if (current.length() > 0)
			lines.add(current.toString());
		return "\"" + String.join("\\n", lines) + "\"";
	}
}
