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
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
 * When a model has a parent template, each ancestor is rendered in its own
 * labelled subgraph cluster (including {@code AbstractSupport} placeholders).
 * Concrete elements that override an abstract support are rendered outside the
 * cluster, connected to their placeholder by a dashed inv/inv arrow.
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

	/** DOT graph-level attributes applied to every template cluster. */
	private static final String CLUSTER_ATTRS = "style=filled; fillcolor=lightyellow; color=grey;";

	/**
	 * Suffix appended to the DOT node id of an abstract support that was
	 * overridden in the current model. Keeps the node distinct from the
	 * concrete element that fulfils it.
	 */
	private static final String ABSTRACT_SUFFIX = "@abstract";

	// -------------------------------------------------------------------------

	private final StringBuilder builder = new StringBuilder();
	private String currentModelName;

	/**
	 * Ids overridden by the current model itself ({@code concreteOverrides()}).
	 * Used for leaf-edge ghost routing in the direct-supports path.
	 */
	private Set<String> overriddenIds;

	/**
	 * Union of {@code concreteOverrides()} ids across the full ancestor chain
	 * (current model + all ancestor templates). An {@link AbstractSupport} node
	 * in any cluster gets {@link #ABSTRACT_SUFFIX} iff its child-qualified id
	 * is in this set. Also drives the full set of override arrows.
	 */
	private Set<String> allOverriddenIds;

	/**
	 * Ids of elements copied unchanged from an ancestor template
	 * ({@code inheritedElements()}). Inherited strategies route their leaf
	 * edges through the abstract ghost node rather than the concrete override.
	 */
	private Set<String> inheritedIds;

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
	// Element visit methods (for elements rendered outside clusters)
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
	// Core export logic
	// ---------------------------------------------------------------------------

	private void exportModelBody(JustificationModel<?> model) {
		currentModelName = model.getName();
		overriddenIds = model.concreteOverrides().stream()
				.map(JustificationElement::id).collect(toSet());
		inheritedIds = model.inheritedElements().stream()
				.map(JustificationElement::id).collect(toSet());
		allOverriddenIds = new HashSet<>(overriddenIds);
		JustificationModel<?> cursor = model.getParent().orElse(null);
		while (cursor != null) {
			cursor.concreteOverrides().stream().map(JustificationElement::id)
					.forEach(allOverriddenIds::add);
			cursor = cursor.getParent().orElse(null);
		}

		builder.append("digraph ").append(quoted(model.getName())).append(" {")
				.append(System.lineSeparator());
		builder.append(INDENT).append("rankdir=BT;")
				.append(System.lineSeparator());
		builder.append(INDENT).append("label=")
				.append(wrapAndQuoteLabel(displayLabel(model))).append(";")
				.append(System.lineSeparator());

		// Own conclusion, own elements, and concrete overrides — rendered
		// outside clusters
		if (model.hasOwnConclusion()) {
			model.conclusion().ifPresent(c -> c.accept(this));
		}
		model.ownElements().forEach(e -> e.accept(this));
		model.concreteOverrides().forEach(e -> e.accept(this));

		// One cluster per ancestor level. Each cluster contains that ancestor's
		// own
		// elements (defined directly in it, not inherited from its own
		// parents),
		// including AbstractSupport placeholders. Overridden abstract supports
		// receive
		// ABSTRACT_SUFFIX so their DOT node id does not clash with the concrete
		// element.
		JustificationModel<?> ancestor = model.getParent().orElse(null);
		while (ancestor != null) {
			final JustificationModel<?> anc = ancestor;
			String ancName = anc.getName();

			builder.append(INDENT).append("subgraph cluster_").append(ancName)
					.append(" {").append(System.lineSeparator());
			builder.append(INDENT).append(INDENT).append(CLUSTER_ATTRS)
					.append(System.lineSeparator());
			builder.append(INDENT).append(INDENT).append("label=")
					.append(wrapAndQuoteLabel(displayLabel(anc))).append(";")
					.append(System.lineSeparator());

			if (anc.hasOwnConclusion()) {
				anc.conclusion().ifPresent(c -> appendClusterNode(
						qualifyForChild(c.id(), ancName), c));
			}
			anc.ownElements().forEach(e -> appendClusterNode(
					qualifyForChild(e.id(), ancName), e));
			anc.concreteOverrides().forEach(e -> appendClusterNode(
					qualifyForChild(e.id(), ancName), e));

			builder.append(INDENT).append("}").append(System.lineSeparator());
			ancestor = anc.getParent().orElse(null);
		}

		// Override arrows: concrete → abstract ghost, across the full chain
		allOverriddenIds.forEach(this::appendOverrideArrow);

		exportEdges(model);
		builder.append("}").append(System.lineSeparator());
	}

	private void exportEdges(JustificationModel<?> model) {
		model.conclusion().ifPresent(c -> c.getSupport()
				.ifPresent(s -> appendEdge(qualify(s.id()), qualify(c.id()))));

		model.subConclusions().forEach(sc -> sc.getSupport()
				.ifPresent(s -> appendEdge(qualify(s.id()), qualify(sc.id()))));

		model.strategies().forEach(s -> leafEdgeSources(model, s)
				.forEach(sourceId -> appendEdge(sourceId, qualify(s.id()))));
	}

	/**
	 * Returns the DOT node ids of the leaves (edge sources) for the given
	 * strategy.
	 *
	 * <p>
	 * When the strategy has direct supports (concrete model elements), each
	 * leaf is qualified with the current model name — these are always concrete
	 * nodes.
	 *
	 * <p>
	 * When the support list is empty — because {@link AbstractSupport} leaves
	 * were excluded during template expansion for {@link Justification} models
	 * — the parent template is consulted. Leaves that were overridden in the
	 * current model resolve to the abstract ghost node
	 * ({@link #ABSTRACT_SUFFIX}) because the template structural edge points to
	 * the placeholder, not the concrete fulfilment.
	 */
	private List<String> leafEdgeSources(JustificationModel<?> model,
			Strategy strategy) {
		if (!strategy.getSupports().isEmpty()) {
			boolean isInherited = inheritedIds.contains(strategy.id());
			return strategy.getSupports().stream()
					.map(leaf -> ((JustificationElement) leaf).id())
					.map(leafId -> (isInherited
							&& allOverriddenIds.contains(leafId))
									? qualify(leafId) + ABSTRACT_SUFFIX
									: qualify(leafId))
					.toList();
		}
		return model.getParent().flatMap(parent -> {
			String idInParent = stripParentPrefix(strategy.id(),
					parent.getName());
			return parent.findById(idInParent)
					.filter(Strategy.class::isInstance)
					.map(Strategy.class::cast)
					.map(ps -> ps.getSupports().stream()
							.map(leaf -> ((JustificationElement) leaf).id())
							.map(leafId -> qualifyForChild(leafId,
									parent.getName()))
							.map(qid -> allOverriddenIds.contains(qid)
									? qualify(qid) + ABSTRACT_SUFFIX
									: qualify(qid))
							.toList());
		}).orElse(List.of());
	}

	// ---------------------------------------------------------------------------
	// Private helpers
	// ---------------------------------------------------------------------------

	private void appendNode(JustificationElement element, String attrs) {
		String qid = qualify(element.id());
		builder.append(INDENT).append(quoted(qid)).append(" [label=")
				.append(wrapAndQuoteLabel(element.label())).append(", id=")
				.append(quoted(qid)).append(", ").append(attrs).append("];")
				.append(System.lineSeparator());
	}

	/**
	 * Appends a node declaration for an ancestor element inside a cluster.
	 * Overridden abstract supports receive {@link #ABSTRACT_SUFFIX} on their
	 * DOT node name so they do not clash with the concrete replacement rendered
	 * outside the cluster.
	 */
	private void appendClusterNode(String childQualifiedId,
			JustificationElement element) {
		boolean isGhost = element instanceof AbstractSupport
				&& allOverriddenIds.contains(childQualifiedId);
		String dotNodeId = qualify(childQualifiedId)
				+ (isGhost ? ABSTRACT_SUFFIX : "");
		String semanticId = qualify(childQualifiedId);
		builder.append(INDENT).append(quoted(dotNodeId)).append(" [label=")
				.append(wrapAndQuoteLabel(element.label())).append(", id=")
				.append(quoted(semanticId)).append(", ")
				.append(styleForElement(element).toAttrs()).append("];")
				.append(System.lineSeparator());
	}

	private void appendEdge(String fromId, String toId) {
		builder.append(INDENT).append(quoted(fromId)).append(" -> ")
				.append(quoted(toId)).append(";")
				.append(System.lineSeparator());
	}

	/**
	 * Appends a dotted open-headed arrow from the concrete override element to
	 * its abstract support placeholder, showing that the concrete element
	 * fulfils the template contract.
	 */
	private void appendOverrideArrow(String elementId) {
		String concreteId = qualify(elementId);
		String abstractId = concreteId + ABSTRACT_SUFFIX;
		builder.append(INDENT).append(quoted(concreteId)).append(" -> ")
				.append(quoted(abstractId))
				.append(" [arrowhead=empty, style=dotted];")
				.append(System.lineSeparator());
	}

	/** Returns the {@link NodeStyle} for the given element type. */
	private static NodeStyle styleForElement(JustificationElement element) {
		if (element instanceof Conclusion)
			return CONCLUSION_STYLE;
		if (element instanceof SubConclusion)
			return SUB_CONCLUSION_STYLE;
		if (element instanceof Strategy)
			return STRATEGY_STYLE;
		if (element instanceof Evidence)
			return EVIDENCE_STYLE;
		return ABSTRACT_SUPPORT_STYLE; // AbstractSupport
	}

	/**
	 * Returns the display label for {@code model}: template models are prefixed
	 * with {@code <<template>>} to distinguish them from concrete
	 * justifications.
	 */
	private static String displayLabel(JustificationModel<?> model) {
		return model instanceof Template
				? "<<template>> " + model.getName()
				: model.getName();
	}

	/**
	 * Strips the parent model name prefix from a qualified element id. E.g.,
	 * {@code "parent:s"} with parent name {@code "parent"} returns {@code "s"}.
	 * Ids that do not start with {@code parentName:} (grandparent-qualified ids
	 * such as {@code "t1:s"}) are returned unchanged.
	 */
	private static String stripParentPrefix(String id, String parentName) {
		String prefix = parentName + ":";
		return id.startsWith(prefix) ? id.substring(prefix.length()) : id;
	}

	/**
	 * Mirrors {@link JustificationModel#qualifiedCopy}: plain ids become
	 * {@code prefix:id}; already-qualified ids (grandparent elements) are kept
	 * as-is.
	 */
	private static String qualifyForChild(String id, String prefix) {
		return id.contains(":") ? id : prefix + ":" + id;
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
