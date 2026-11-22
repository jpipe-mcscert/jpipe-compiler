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
 * When a model has a parent template, each ancestor is rendered in its own
 * labelled subgraph cluster (including {@code AbstractSupport} placeholders).
 * Concrete elements that override an abstract support are rendered outside the
 * cluster, connected to their placeholder by a dashed inv/inv arrow.
 *
 * <p>
 * Node visual styles are defined in {@link DotNodeStyle}. Label escaping and
 * word-wrapping utilities live in {@link DotLabel}.
 */
public class DotExporter extends AbstractModelExporter {

	static final String INDENT = "  ";

	/** DOT graph-level attributes applied to every template cluster. */
	static final String CLUSTER_ATTRS = "style=filled; fillcolor=lightyellow; color=grey;";

	/**
	 * Suffix appended to the DOT node id of an abstract support that was
	 * overridden in the current model. Keeps the node distinct from the
	 * concrete element that fulfils it.
	 */
	static final String ABSTRACT_SUFFIX = "@abstract";

	// -------------------------------------------------------------------------
	// Per-export state (reset by exportModel)
	// -------------------------------------------------------------------------

	/** Where each element of the model being exported is drawn. */
	private TemplateLayout layout;

	// -------------------------------------------------------------------------

	/**
	 * Serialise {@code model} to DOT text.
	 *
	 * @param model
	 *            the justification or template to serialise.
	 * @return DOT source text ready to be piped to {@code dot}.
	 */
	public String export(JustificationModel model) {
		builder.setLength(0);
		model.accept(this);
		return builder.toString();
	}

	// -------------------------------------------------------------------------
	// Element visit methods
	// -------------------------------------------------------------------------

	@Override
	public Void visit(Conclusion conclusion) {
		appendNode(conclusion);
		return null;
	}

	@Override
	public Void visit(SubConclusion subConclusion) {
		appendNode(subConclusion);
		return null;
	}

	@Override
	public Void visit(Strategy strategy) {
		appendNode(strategy);
		return null;
	}

	@Override
	public Void visit(Evidence evidence) {
		appendNode(evidence);
		return null;
	}

	@Override
	public Void visit(AbstractSupport abstractSupport) {
		appendNode(abstractSupport);
		return null;
	}

	// -------------------------------------------------------------------------
	// Core export logic (AbstractModelExporter)
	// -------------------------------------------------------------------------

	@Override
	protected void exportModel(JustificationModel model) {
		currentModelName = model.getName();
		layout = TemplateLayout.of(model);

		builder.append("digraph ").append(DotLabel.quoted(model.getName()))
				.append(" {").append(System.lineSeparator());
		builder.append(INDENT).append("rankdir=BT;")
				.append(System.lineSeparator());
		builder.append(INDENT).append("label=")
				.append(DotLabel.wrapAndQuote(DotLabel.display(model)))
				.append(";").append(System.lineSeparator());

		// Own conclusion, own elements, and concrete overrides — outside
		// clusters
		layout.outside().forEach(e -> e.accept(this));

		// Ancestor clusters nested to mirror the inheritance hierarchy
		appendClusters(layout.clusters());

		// Override arrows: concrete → abstract ghost, in drawing order
		layout.placeholders()
				.forEach(placeholder -> appendOverrideArrow(placeholder.id()));

		exportEdges(model);
		builder.append("}").append(System.lineSeparator());
	}

	// -------------------------------------------------------------------------
	// Edge rendering
	// -------------------------------------------------------------------------

	private void exportEdges(JustificationModel model) {
		model.forEachSupportEdge((supported, supporter) -> appendEdge(
				edgeSource(supporter, supported), qualify(supported)));
	}

	/**
	 * The DOT node id an edge from {@code supporter} to {@code supported} is
	 * drawn from.
	 *
	 * <p>
	 * Template expansion copies every leaf, {@link AbstractSupport}
	 * placeholders included, so an inherited strategy keeps its supports. A
	 * strategy with no support draws no edge: borrowing the leaves of a parent
	 * strategy with the same short id would draw edges the model never declares
	 * (#189).
	 *
	 * <p>
	 * A leaf of an inherited strategy that is overridden resolves to the
	 * abstract ghost node ({@link #ABSTRACT_SUFFIX}), because the template
	 * structural edge points to the placeholder, not the concrete fulfilment
	 * (see {@link TemplateLayout#throughPlaceholder}). Only a leaf fills a
	 * placeholder, so the edges into a conclusion or a sub-conclusion, drawn
	 * from a strategy, never do.
	 */
	private String edgeSource(String supporter, String supported) {
		return layout.throughPlaceholder(supporter, supported)
				? qualify(supporter) + ABSTRACT_SUFFIX
				: qualify(supporter);
	}

	// -------------------------------------------------------------------------
	// Cluster rendering
	// -------------------------------------------------------------------------

	/**
	 * Appends a {@code subgraph cluster_<name>} block per ancestor, each nested
	 * in the one before it, so that the clusters mirror the inheritance
	 * hierarchy.
	 */
	private void appendClusters(List<TemplateLayout.Cluster> clusters) {
		String indent = INDENT;
		for (TemplateLayout.Cluster cluster : clusters) {
			JustificationModel ancestor = cluster.template();
			builder.append(indent).append("subgraph ")
					.append(DotLabel.quoted("cluster_" + ancestor.getName()))
					.append(" {").append(System.lineSeparator());
			builder.append(indent).append(INDENT).append(CLUSTER_ATTRS)
					.append(System.lineSeparator());
			builder.append(indent).append(INDENT).append("label=")
					.append(DotLabel.wrapAndQuote(DotLabel.display(ancestor)))
					.append(";").append(System.lineSeparator());
			String inner = indent + INDENT;
			cluster.nodes().forEach(node -> appendClusterNode(node, inner));
			indent = inner;
		}
		for (int i = clusters.size(); i > 0; i--) {
			builder.append(INDENT.repeat(i)).append("}")
					.append(System.lineSeparator());
		}
	}

	/**
	 * Appends a node declaration for an ancestor element inside a cluster.
	 * Overridden abstract supports receive {@link #ABSTRACT_SUFFIX} on their
	 * DOT node name and on their {@code id} attribute, so that neither clashes
	 * with the concrete replacement rendered outside the cluster: an SVG with
	 * two equal ids lets {@code getElementById} reach only one of them.
	 */
	private void appendClusterNode(TemplateLayout.Node node, String indent) {
		String dotNodeId = qualify(node.id())
				+ (node.placeholder() ? ABSTRACT_SUFFIX : "");
		builder.append(indent).append(DotLabel.quoted(dotNodeId))
				.append(" [label=")
				.append(DotLabel.wrapAndQuote(node.element().label()))
				.append(", id=").append(DotLabel.quoted(dotNodeId)).append(", ")
				.append(DotNodeStyle.of(node.element()).toAttrs()).append("];")
				.append(System.lineSeparator());
	}

	// -------------------------------------------------------------------------
	// Node and edge primitives
	// -------------------------------------------------------------------------

	private void appendNode(JustificationElement element) {
		String attrs = DotNodeStyle.of(element).toAttrs();
		String qid = qualify(element.id());
		builder.append(INDENT).append(DotLabel.quoted(qid)).append(" [label=")
				.append(DotLabel.wrapAndQuote(element.label())).append(", id=")
				.append(DotLabel.quoted(qid)).append(", ").append(attrs)
				.append("];").append(System.lineSeparator());
	}

	private void appendEdge(String fromId, String toId) {
		builder.append(INDENT).append(DotLabel.quoted(fromId)).append(" -> ")
				.append(DotLabel.quoted(toId)).append(";")
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
		builder.append(INDENT).append(DotLabel.quoted(concreteId))
				.append(" -> ").append(DotLabel.quoted(abstractId))
				.append(" [arrowhead=empty, style=dotted];")
				.append(System.lineSeparator());
	}
}
