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

	private final StringBuilder builder = new StringBuilder();

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
		appendNode(conclusion,
				"shape=rect, style=\"filled,rounded\", fillcolor=lightgrey");
		return null;
	}

	@Override
	public Void visit(SubConclusion subConclusion) {
		appendNode(subConclusion, "shape=rect, color=dodgerblue");
		return null;
	}

	@Override
	public Void visit(Strategy strategy) {
		appendNode(strategy,
				"shape=parallelogram, style=filled, fillcolor=palegreen");
		return null;
	}

	@Override
	public Void visit(Evidence evidence) {
		appendNode(evidence,
				"shape=note, style=filled, fillcolor=lightskyblue2");
		return null;
	}

	@Override
	public Void visit(AbstractSupport abstractSupport) {
		appendNode(abstractSupport, "shape=rect, style=dotted");
		return null;
	}

	// ---------------------------------------------------------------------------
	// Private helpers
	// ---------------------------------------------------------------------------

	private void exportModelBody(JustificationModel<?> model) {
		builder.append("digraph ").append(quoted(model.getName())).append(" {")
				.append(System.lineSeparator());
		builder.append(INDENT).append("rankdir=BT;")
				.append(System.lineSeparator());
		builder.append(INDENT).append("label=").append(quoted(model.getName()))
				.append(";").append(System.lineSeparator());
		model.conclusion().ifPresent(c -> c.accept(this));
		model.getElements().forEach(e -> e.accept(this));
		exportEdges(model);
		builder.append("}").append(System.lineSeparator());
	}

	private void exportEdges(JustificationModel<?> model) {
		model.conclusion().ifPresent(
				c -> c.getSupport().ifPresent(s -> appendEdge(s.id(), c.id())));
		model.subConclusions().forEach(sc -> sc.getSupport()
				.ifPresent(s -> appendEdge(s.id(), sc.id())));
		model.strategies().forEach(s -> s.getSupport().ifPresent(
				leaf -> appendEdge(((JustificationElement) leaf).id(),
						s.id())));
	}

	private void appendNode(JustificationElement element, String attrs) {
		builder.append(INDENT).append(quoted(element.id())).append(" [label=")
				.append(quoted(element.label())).append(", ").append(attrs)
				.append("];").append(System.lineSeparator());
	}

	private void appendEdge(String fromId, String toId) {
		builder.append(INDENT).append(quoted(fromId)).append(" -> ")
				.append(quoted(toId)).append(";")
				.append(System.lineSeparator());
	}

	private static String quoted(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}
}
