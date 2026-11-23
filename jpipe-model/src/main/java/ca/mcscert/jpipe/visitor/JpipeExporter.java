package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.Composition;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serialises a compiled jPipe model back to {@code .jd} source text conforming
 * to the jPipe grammar.
 *
 * <p>
 * The output is a round-trip: compiling it gives back the model exported, with
 * the same elements, ids, relations and aliases. To that end:
 * <ul>
 * <li>the output is self-contained: it holds, before the model, every model it
 * depends on, the template it implements and the models it was composed from,
 * each once, dependencies first, and never a {@code load};</li>
 * <li>a model that implements a template declares only what its source did: its
 * own elements, its overrides and its own relations, not what it inherits;</li>
 * <li>a composed model is written as the operator call that built it;</li>
 * <li>a model loaded under a namespace, which the grammar cannot name, is
 * renamed to a plain identifier, together with the ids that name it (see
 * {@link PlainModelNames}).</li>
 * </ul>
 *
 * <p>
 * Each element visit method writes one declaration. {@link #export(Unit)}
 * writes every model of the unit, dependencies first.
 */
public class JpipeExporter implements JustificationVisitor<Void> {

	private static final String INDENT = "  ";

	private final StringBuilder builder = new StringBuilder();

	private PlainModelNames names = PlainModelNames.of(List.of());

	/** The model being written, which the ids of its body belong to. */
	private JustificationModel current;

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
	 * Serialise a single {@code model} to {@code .jd} source text, preceded by
	 * the models it depends on.
	 *
	 * @param model
	 *            the model to serialise.
	 * @return well-formed, self-contained {@code .jd} text for this model.
	 */
	public String export(JustificationModel model) {
		return write(List.of(model));
	}

	/**
	 * The name {@link #export} gives {@code model} once its dependencies are
	 * written with it: its own, unless it was loaded under a namespace.
	 */
	public static PlainModelNames namesFor(JustificationModel model) {
		return namesOf(withDependencies(List.of(model)));
	}

	private static PlainModelNames namesOf(Set<JustificationModel> ordered) {
		return PlainModelNames
				.of(ordered.stream().map(JustificationModel::getName).toList());
	}

	private String write(Collection<? extends JustificationModel> models) {
		builder.setLength(0);
		appendModels(models);
		return builder.toString();
	}

	/**
	 * Appends {@code models} and every model they depend on, dependencies
	 * first, separated by blank lines, under the names they are given together.
	 */
	private void appendModels(Collection<? extends JustificationModel> models) {
		Set<JustificationModel> ordered = withDependencies(models);
		names = namesOf(ordered);
		String separator = "";
		for (JustificationModel model : ordered) {
			builder.append(separator);
			model.accept(this);
			separator = System.lineSeparator();
		}
	}

	/**
	 * {@code models} and every model they depend on, each once, a model after
	 * the ones it depends on.
	 */
	private static Set<JustificationModel> withDependencies(
			Collection<? extends JustificationModel> models) {
		Set<JustificationModel> ordered = new LinkedHashSet<>();
		models.forEach(m -> addWithDependencies(m, ordered));
		return ordered;
	}

	private static void addWithDependencies(JustificationModel model,
			Set<JustificationModel> ordered) {
		if (ordered.contains(model)) {
			return;
		}
		model.getParent().ifPresent(p -> addWithDependencies(p, ordered));
		model.composition().ifPresent(c -> c.sources()
				.forEach(source -> addWithDependencies(source, ordered)));
		ordered.add(model);
	}

	// -------------------------------------------------------------------------
	// Unit and model visit methods
	// -------------------------------------------------------------------------

	/** Writes every model of the unit, as {@link #export(Unit)} does. */
	@Override
	public Void visit(Unit unit) {
		appendModels(unit.getModels());
		return null;
	}

	@Override
	public Void visit(Justification justification) {
		exportModel("justification", justification);
		return null;
	}

	@Override
	public Void visit(Template template) {
		exportModel("template", template);
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

	private void exportModel(String keyword, JustificationModel model) {
		current = model;
		builder.append(keyword).append(" ").append(names.name(model.getName()));
		model.composition().ifPresentOrElse(this::appendCall,
				() -> appendBody(model));
	}

	/**
	 * {@code is op(a, b) { key: "value" }}, the configuration block left out
	 * when the call has no argument, as the grammar requires one entry at
	 * least.
	 */
	private void appendCall(Composition composition) {
		builder.append(" is ").append(composition.operator()).append("(")
				.append(composition.sources().stream()
						.map(s -> names.name(s.getName()))
						.collect(Collectors.joining(", ")))
				.append(")");
		if (composition.arguments().isEmpty()) {
			builder.append(System.lineSeparator());
			return;
		}
		builder.append(" {").append(System.lineSeparator());
		composition.arguments().forEach((key, value) -> {
			Composition.Scope scope = composition.idArguments().get(key);
			builder.append(INDENT).append(key).append(": ")
					.append(Quoting.doubleQuoted(scope == null
							? value
							: renameIds(composition, scope, value)))
					.append(System.lineSeparator());
		});
		builder.append("}").append(System.lineSeparator());
	}

	/**
	 * A comma-separated list of ids, each renamed as an id of the result or of
	 * a source, as {@code scope} says.
	 */
	private String renameIds(Composition composition, Composition.Scope scope,
			String value) {
		return Arrays.stream(value.split(",", -1)).map(String::strip)
				.map(id -> switch (scope) {
					case RESULT ->
						names.resultArgument(composition.sources(), id);
					case SOURCE ->
						names.sourceArgument(composition.sources(), id);
				}).collect(Collectors.joining(","));
	}

	/**
	 * The model's declarations: for a model that implements a template, only
	 * the elements and relations it does not inherit.
	 */
	private void appendBody(JustificationModel model) {
		model.getParent().ifPresent(p -> builder.append(" implements ")
				.append(names.name(p.getName())));
		builder.append(" {").append(System.lineSeparator());

		if (model.hasOwnConclusion()) {
			model.conclusion().ifPresent(c -> c.accept(this));
		}
		model.declaredElements().forEach(e -> e.accept(this));

		Set<List<String>> inheritedEdges = inheritedEdges(model);
		model.forEachSupportEdge((supported, supporter) -> {
			if (!inheritedEdges.contains(List.of(supported, supporter))) {
				appendRelation(supporter, supported);
			}
		});

		builder.append("}").append(System.lineSeparator());
	}

	/**
	 * The support edges {@code model} inherits, as {@code [supported,
	 * supporter]} pairs of the ids it gives them: its template's edges, each
	 * end named as the model's copy of it (see
	 * {@link JustificationModel#copyOf}). An override keeps the id of the
	 * placeholder it replaces, so the edges it was rewired into are inherited
	 * too.
	 */
	private static Set<List<String>> inheritedEdges(JustificationModel model) {
		Set<List<String>> edges = new HashSet<>();
		model.getParent().ifPresent(p -> p.forEachSupportEdge((supported,
				supporter) -> edges.add(List.of(copyIn(model, p, supported),
						copyIn(model, p, supporter)))));
		return edges;
	}

	/**
	 * The id {@code model} gives its copy of {@code parent}'s element
	 * {@code id}: a model holds a copy of every element of its template, its
	 * conclusion included (ADR-0012).
	 */
	private static String copyIn(JustificationModel model,
			JustificationModel parent, String id) {
		return model.copyOf(parent, id).orElseThrow();
	}

	private void appendElement(String keyword, JustificationElement element) {
		builder.append(INDENT).append(keyword).append(" ")
				.append(names.id(current, element.id())).append(" is ")
				.append(Quoting.doubleQuoted(element.label()))
				.append(System.lineSeparator());
	}

	private void appendRelation(String supporterId, String supportedId) {
		builder.append(INDENT).append(names.id(current, supporterId))
				.append(" supports ").append(names.id(current, supportedId))
				.append(System.lineSeparator());
	}

}
