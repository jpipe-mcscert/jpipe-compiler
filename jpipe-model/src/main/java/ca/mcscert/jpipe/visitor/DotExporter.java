package ca.mcscert.jpipe.visitor;

import static java.util.stream.Collectors.toSet;

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
 *
 * <p>
 * Node visual styles are defined in {@link DotNodeStyle}. Label escaping and
 * word-wrapping utilities live in {@link DotLabel}.
 */
public class DotExporter extends AbstractModelExporter {

	private static final String INDENT = "  ";

	/** DOT graph-level attributes applied to every template cluster. */
	private static final String CLUSTER_ATTRS = "style=filled; fillcolor=lightyellow; color=grey;";

	/**
	 * Suffix appended to the DOT node id of an abstract support that was
	 * overridden in the current model. Keeps the node distinct from the
	 * concrete element that fulfils it.
	 */
	private static final String ABSTRACT_SUFFIX = "@abstract";

	// -------------------------------------------------------------------------
	// Per-export state (reset by exportModel)
	// -------------------------------------------------------------------------

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

	// -------------------------------------------------------------------------

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

	// -------------------------------------------------------------------------
	// Element visit methods
	// -------------------------------------------------------------------------

	@Override
	public Void visit(Conclusion conclusion) {
		appendNode(conclusion, DotNodeStyle.CONCLUSION.toAttrs());
		return null;
	}

	@Override
	public Void visit(SubConclusion subConclusion) {
		appendNode(subConclusion, DotNodeStyle.SUB_CONCLUSION.toAttrs());
		return null;
	}

	@Override
	public Void visit(Strategy strategy) {
		appendNode(strategy, DotNodeStyle.STRATEGY.toAttrs());
		return null;
	}

	@Override
	public Void visit(Evidence evidence) {
		appendNode(evidence, DotNodeStyle.EVIDENCE.toAttrs());
		return null;
	}

	@Override
	public Void visit(AbstractSupport abstractSupport) {
		appendNode(abstractSupport, DotNodeStyle.ABSTRACT_SUPPORT.toAttrs());
		return null;
	}

	// -------------------------------------------------------------------------
	// Core export logic (AbstractModelExporter)
	// -------------------------------------------------------------------------

	@Override
	protected void exportModel(JustificationModel<?> model) {
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

		builder.append("digraph ").append(DotLabel.quoted(model.getName()))
				.append(" {").append(System.lineSeparator());
		builder.append(INDENT).append("rankdir=BT;")
				.append(System.lineSeparator());
		builder.append(INDENT).append("label=")
				.append(DotLabel.wrapAndQuote(DotLabel.display(model)))
				.append(";").append(System.lineSeparator());

		// Own conclusion, own elements, and concrete overrides — outside
		// clusters
		if (model.hasOwnConclusion()) {
			model.conclusion().ifPresent(c -> c.accept(this));
		}
		model.ownElements().forEach(e -> e.accept(this));
		model.concreteOverrides().forEach(e -> e.accept(this));

		// Ancestor clusters nested to mirror the inheritance hierarchy
		model.getParent().ifPresent(p -> appendAncestorCluster(p, INDENT));

		// Override arrows: concrete → abstract ghost, across the full chain
		allOverriddenIds.forEach(this::appendOverrideArrow);

		exportEdges(model);
		builder.append("}").append(System.lineSeparator());
	}

	// -------------------------------------------------------------------------
	// Edge rendering
	// -------------------------------------------------------------------------

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

	// -------------------------------------------------------------------------
	// Cluster rendering
	// -------------------------------------------------------------------------

	/**
	 * Recursively appends a {@code subgraph cluster_<name>} block for
	 * {@code ancestor}, then nests the grandparent's cluster inside it before
	 * closing. This produces properly nested DOT clusters that mirror the
	 * inheritance hierarchy.
	 */
	private void appendAncestorCluster(JustificationModel<?> ancestor,
			String indent) {
		String ancName = ancestor.getName();

		builder.append(indent).append("subgraph cluster_").append(ancName)
				.append(" {").append(System.lineSeparator());
		builder.append(indent).append(INDENT).append(CLUSTER_ATTRS)
				.append(System.lineSeparator());
		builder.append(indent).append(INDENT).append("label=")
				.append(DotLabel.wrapAndQuote(DotLabel.display(ancestor)))
				.append(";").append(System.lineSeparator());

		String inner = indent + INDENT;
		if (ancestor.hasOwnConclusion()) {
			ancestor.conclusion().ifPresent(c -> appendClusterNode(
					qualifyForChild(c.id(), ancName), c, inner));
		}
		ancestor.ownElements().forEach(e -> appendClusterNode(
				qualifyForChild(e.id(), ancName), e, inner));
		ancestor.concreteOverrides().forEach(e -> appendClusterNode(
				qualifyForChild(e.id(), ancName), e, inner));

		// Recurse: grandparent cluster is nested inside this one
		ancestor.getParent().ifPresent(gp -> appendAncestorCluster(gp, inner));

		builder.append(indent).append("}").append(System.lineSeparator());
	}

	/**
	 * Appends a node declaration for an ancestor element inside a cluster.
	 * Overridden abstract supports receive {@link #ABSTRACT_SUFFIX} on their
	 * DOT node name so they do not clash with the concrete replacement rendered
	 * outside the cluster.
	 */
	private void appendClusterNode(String childQualifiedId,
			JustificationElement element, String indent) {
		boolean isGhost = element instanceof AbstractSupport
				&& allOverriddenIds.contains(childQualifiedId);
		String dotNodeId = qualify(childQualifiedId)
				+ (isGhost ? ABSTRACT_SUFFIX : "");
		String semanticId = qualify(childQualifiedId);
		builder.append(indent).append(DotLabel.quoted(dotNodeId))
				.append(" [label=")
				.append(DotLabel.wrapAndQuote(element.label())).append(", id=")
				.append(DotLabel.quoted(semanticId)).append(", ")
				.append(DotNodeStyle.of(element).toAttrs()).append("];")
				.append(System.lineSeparator());
	}

	// -------------------------------------------------------------------------
	// Node and edge primitives
	// -------------------------------------------------------------------------

	private void appendNode(JustificationElement element, String attrs) {
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

	// -------------------------------------------------------------------------
	// ID helpers
	// -------------------------------------------------------------------------

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
	 * Mirrors {@link ca.mcscert.jpipe.model.JustificationModel#qualifiedCopy}:
	 * plain ids become {@code prefix:id}; already-qualified ids (grandparent
	 * elements) are kept as-is.
	 */
	private static String qualifyForChild(String id, String prefix) {
		return id.contains(":") ? id : prefix + ":" + id;
	}
}
