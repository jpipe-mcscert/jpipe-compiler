package ca.mcscert.jpipe.visitor;

import static ca.mcscert.jpipe.util.QualifiedIds.qualifyIfPlain;

import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * How a model that implements a template is drawn: which of its elements are
 * drawn outside every template, which in each ancestor template's cluster, and
 * which {@code @support} placeholders a cluster shows next to the elements that
 * fill them. {@link DotExporter} draws it, and {@link JsonExporter} writes it,
 * so that the DOT export can be rebuilt from the JSON one (#199).
 *
 * <p>
 * Ids are the model's own, not yet qualified with its name: an ancestor's
 * element is named as template expansion names it, {@code template:id}, or kept
 * as is when it was already qualified by a further ancestor.
 */
final class TemplateLayout {

	/**
	 * An element drawn in a cluster.
	 *
	 * @param id
	 *            the element's id in the model.
	 * @param element
	 *            the ancestor's element, with the label and kind drawn.
	 * @param placeholder
	 *            whether it is a {@code @support} the model, or a template
	 *            between it and the ancestor, fills: the element of the model
	 *            with the same id is drawn elsewhere, and the two are linked.
	 */
	record Node(String id, JustificationElement element, boolean placeholder) {
	}

	/**
	 * An ancestor template's cluster.
	 *
	 * @param template
	 *            the ancestor.
	 * @param nodes
	 *            the elements drawn in it, in drawing order.
	 */
	record Cluster(JustificationModel template, List<Node> nodes) {
	}

	private final JustificationModel model;
	private final List<JustificationElement> outside;
	private final List<Cluster> clusters;
	private final Set<String> filled;
	private final Set<String> inherited;

	private TemplateLayout(JustificationModel model) {
		this.model = model;
		this.filled = filledIds(model);
		this.inherited = new HashSet<>();
		model.inheritedElements().forEach(e -> inherited.add(e.id()));
		this.outside = new ArrayList<>();
		if (model.hasOwnConclusion()) {
			model.conclusion().ifPresent(outside::add);
		}
		outside.addAll(model.ownElements());
		outside.addAll(model.concreteOverrides());
		this.clusters = new ArrayList<>();
		JustificationModel ancestor = model.getParent().orElse(null);
		while (ancestor != null) {
			clusters.add(clusterOf(ancestor));
			ancestor = ancestor.getParent().orElse(null);
		}
	}

	/** The layout of {@code model}. */
	static TemplateLayout of(JustificationModel model) {
		return new TemplateLayout(model);
	}

	/**
	 * The elements drawn outside every cluster: the model's own conclusion, its
	 * own elements, then the {@code @support}s it fills.
	 */
	List<JustificationElement> outside() {
		return List.copyOf(outside);
	}

	/**
	 * One cluster per ancestor template, the parent's first: each one is drawn
	 * inside the one before it.
	 */
	List<Cluster> clusters() {
		return List.copyOf(clusters);
	}

	/** The placeholders of every cluster, in drawing order. */
	List<Node> placeholders() {
		return clusters.stream().flatMap(c -> c.nodes().stream())
				.filter(Node::placeholder).toList();
	}

	/**
	 * Whether the edge from {@code leafId} to {@code strategyId} is drawn from
	 * the placeholder {@code leafId} fills rather than from the element: the
	 * strategy is the template's, so the edge is the template's, and leads from
	 * the placeholder it declares.
	 */
	boolean throughPlaceholder(String leafId, String strategyId) {
		return inherited.contains(strategyId) && filled.contains(leafId);
	}

	/**
	 * The strategies of the model whose edge from {@code placeholder} is drawn
	 * from it; see throughPlaceholder.
	 */
	List<Strategy> supportedThroughPlaceholder(Node placeholder) {
		return model.strategiesSupportedBy(placeholder.id()).stream()
				.filter(s -> throughPlaceholder(placeholder.id(), s.id()))
				.toList();
	}

	private Cluster clusterOf(JustificationModel ancestor) {
		String name = ancestor.getName();
		List<JustificationElement> drawn = new ArrayList<>();
		if (ancestor.hasOwnConclusion()) {
			ancestor.conclusion().ifPresent(drawn::add);
		}
		drawn.addAll(ancestor.ownElements());
		drawn.addAll(ancestor.concreteOverrides());
		List<Node> nodes = drawn.stream().map(e -> {
			String id = qualifyIfPlain(name, e.id());
			return new Node(id, e,
					e instanceof AbstractSupport && filled.contains(id));
		}).toList();
		return new Cluster(ancestor, nodes);
	}

	/**
	 * The ids of the {@code @support}s filled along the chain: by the model, or
	 * by an ancestor for a template further up.
	 */
	private static Set<String> filledIds(JustificationModel model) {
		Set<String> ids = new HashSet<>();
		JustificationModel cursor = model;
		while (cursor != null) {
			cursor.concreteOverrides().forEach(e -> ids.add(e.id()));
			cursor = cursor.getParent().orElse(null);
		}
		return ids;
	}
}
