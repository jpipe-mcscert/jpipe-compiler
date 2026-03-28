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
 * Serialises a compiled jPipe model back to {@code .jd} source text conforming
 * to the jPipe grammar.
 *
 * <p>
 * Each visitor method is responsible for descending into its children via
 * {@code accept}, so the full traversal is visitor-driven.
 * {@link #export(Unit)} is a convenience wrapper around
 * {@code unit.accept(this)} that resets internal state and returns the
 * accumulated string.
 */
public class JpipeExporter implements JustificationVisitor<Void> {

	private static final String INDENT = "  ";

	private final StringBuilder builder = new StringBuilder();

	/**
	 * Serialise {@code unit} to {@code .jd} source text.
	 *
	 * @param unit
	 *            the compiled unit to serialise.
	 * @return well-formed {@code .jd} text for all models in the unit.
	 */
	public String export(Unit unit) {
		builder.setLength(0);
		unit.accept(this);
		return builder.toString();
	}

	/**
	 * Serialise a single {@code model} to {@code .jd} source text.
	 *
	 * @param model
	 *            the model to serialise.
	 * @return well-formed {@code .jd} text for this model only.
	 */
	public String export(JustificationModel<?> model) {
		builder.setLength(0);
		model.accept(this);
		return builder.toString();
	}

	// -------------------------------------------------------------------------
	// Unit and model visit methods
	// -------------------------------------------------------------------------

	@Override
	public Void visit(Unit unit) {
		unit.getModels().forEach(m -> m.accept(this));
		return null;
	}

	@Override
	public Void visit(Justification justification) {
		exportModelBody("justification", justification);
		return null;
	}

	@Override
	public Void visit(Template template) {
		exportModelBody("template", template);
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
		appendElement("@support", abstractSupport);
		return null;
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	private void exportModelBody(String keyword, JustificationModel<?> model) {
		builder.append(keyword).append(" ").append(model.getName());
		model.getParent().ifPresent(
				p -> builder.append(" implements ").append(p.getName()));
		builder.append(" {").append(System.lineSeparator());

		model.conclusion().ifPresent(c -> c.accept(this));
		model.getElements().forEach(e -> e.accept(this));

		exportSupports(model);

		builder.append("}").append(System.lineSeparator());
	}

	private void exportSupports(JustificationModel<?> model) {
		model.conclusion().ifPresent(c -> c.getSupport()
				.ifPresent(s -> appendRelation(s.id(), c.id())));
		model.subConclusions().forEach(sc -> sc.getSupport()
				.ifPresent(s -> appendRelation(s.id(), sc.id())));
		model.strategies().forEach(s -> s.getSupport().ifPresent(
				leaf -> appendRelation(((JustificationElement) leaf).id(),
						s.id())));
	}

	private void appendElement(String keyword, JustificationElement element) {
		builder.append(INDENT).append(keyword).append(" ").append(element.id())
				.append(" is \"").append(element.label()).append("\"")
				.append(System.lineSeparator());
	}

	private void appendRelation(String supporterId, String supportedId) {
		builder.append(INDENT).append(supporterId).append(" supports ")
				.append(supportedId).append(System.lineSeparator());
	}
}
