package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.util.LabelEscaper;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Serialises a single {@link JustificationModel} to a Python module: each
 * element becomes a decorated function named after the element's label
 * (snake_case via {@link LabelEscaper}), carrying:
 *
 * <ul>
 * <li>one or more {@code @jpipe_link("<id>")}, linking the function back to the
 * originating jPipe element. An element that resulted from a merge is linked by
 * the ids it was merged from rather than by its own, transitively, so a
 * composition of a composition still names the ids as the original source
 * models had them; ids unification minted along the way ({@code unified_0}) are
 * left out, having been written in no source file. Every id is then shortened
 * to the least qualified form that still designates the element unambiguously,
 * but never past {@code container:id}, because a bare id would leave a reader
 * of the generated module unable to tell what it refers to; see
 * {@link AbstractModelExporter#minimalLink(String)}.</li>
 * <li>{@code @jpipe(produce=[], consume=[])} — placeholder decorators to be
 * filled in by the developer.</li>
 * </ul>
 *
 * <p>
 * Each function carries a docstring with the original element label so the full
 * text is always recoverable regardless of escaping.
 *
 * <p>
 * This visitor operates on one model at a time. Use {@code SelectModel} in the
 * compilation pipeline to extract the desired model from a {@link Unit} before
 * invoking this exporter.
 */
public class PythonExporter extends AbstractModelExporter {

	/** Prefix that comments out a line of generated Python. */
	private static final String COMMENT = "# ";

	/**
	 * Link id &rarr; the function it has already been emitted on, for the
	 * module being written. Consumers key their registry by the link id, so the
	 * same id on two functions has no resolution; see
	 * {@link #claim(String, String)}.
	 */
	private final Map<String, String> boundTo = new LinkedHashMap<>();

	/**
	 * Serialise {@code model} to Python source text without a file header.
	 *
	 * @param model
	 *            the justification or template to serialise.
	 * @return Python source text.
	 */
	public String export(JustificationModel<?> model) {
		return export(model, null);
	}

	/**
	 * Serialise {@code model} to Python source text, optionally prefixed with a
	 * generated-file header comment.
	 *
	 * @param model
	 *            the justification or template to serialise.
	 * @param sourcePath
	 *            path of the originating {@code .jd} file, included in the
	 *            header comment; may be {@code null} to omit the header.
	 * @return Python source text.
	 */
	public String export(JustificationModel<?> model, String sourcePath) {
		builder.setLength(0);
		if (sourcePath != null) {
			appendHeader(sourcePath);
		}
		appendPreamble();
		model.accept(this);
		return builder.toString();
	}

	// -------------------------------------------------------------------------
	// Element visit methods
	// -------------------------------------------------------------------------

	/**
	 * Emits nothing. The runner never executes a conclusion: it concludes one
	 * from whatever supports it, so a function here could never run and there
	 * is nothing for a developer to implement. The conclusion remains in the
	 * JSON and DOT exports, which describe the model rather than its execution.
	 */
	@Override
	public Void visit(Conclusion conclusion) {
		return null;
	}

	@Override
	public Void visit(SubConclusion subConclusion) {
		appendMethod(subConclusion);
		return null;
	}

	@Override
	public Void visit(Strategy strategy) {
		appendMethod(strategy);
		return null;
	}

	@Override
	public Void visit(Evidence evidence) {
		appendMethod(evidence);
		return null;
	}

	@Override
	public Void visit(AbstractSupport abstractSupport) {
		appendMethod(abstractSupport);
		return null;
	}

	// -------------------------------------------------------------------------
	// AbstractModelExporter
	// -------------------------------------------------------------------------

	@Override
	protected void exportModel(JustificationModel<?> model) {
		currentModelName = model.getName();
		initAliases(model);
		boundTo.clear();

		// The conclusion is deliberately absent: it is never executed, so it
		// would open a section holding nothing a developer can implement.
		Map<String, List<JustificationElement>> byNamespace = new TreeMap<>();
		model.getElements()
				.forEach(e -> group(byNamespace, e, currentModelName));

		byNamespace.forEach((namespace, elements) -> {
			appendNamespaceBanner(namespace);
			elements.stream()
					.sorted(Comparator
							.comparingInt(PythonExporter::supportRank))
					.forEach(e -> e.accept(this));
		});
	}

	// -------------------------------------------------------------------------
	// Private helpers
	// -------------------------------------------------------------------------

	/**
	 * Orders elements within a namespace bottom-up, so a section reads from the
	 * evidence a developer has to implement toward the claim it ends up
	 * supporting, rather than asking them to start at the conclusion and work
	 * backwards. Ties keep the order the model holds them in.
	 *
	 * <p>
	 * An {@link AbstractSupport} ranks with the evidence: it is a
	 * {@link ca.mcscert.jpipe.model.elements.SupportLeaf} standing where one
	 * will go, and it is the other thing in a module still awaiting an
	 * implementation.
	 */
	private static int supportRank(JustificationElement element) {
		return switch (element) {
			case Evidence _ -> 0;
			case AbstractSupport _ -> 1;
			case Strategy _ -> 2;
			case SubConclusion _ -> 3;
			case Conclusion _ -> 4;
		};
	}

	private static void group(
			Map<String, List<JustificationElement>> byNamespace,
			JustificationElement element, String modelName) {
		byNamespace.computeIfAbsent(namespaceOf(element.id(), modelName),
				_ -> new ArrayList<>()).add(element);
	}

	/**
	 * The namespace an element belongs to: the part of its id in front of the
	 * last segment, or the model itself when the id carries no prefix.
	 *
	 * <p>
	 * An element copied in from a source model or expanded from a template
	 * keeps that origin as its prefix ({@code a_claim:c}, {@code root:abs1}),
	 * so this groups the module the way the author thinks of it. An element the
	 * composition created or merged has no prefix, and belongs to the composed
	 * model itself, being owned by no single source.
	 */
	private static String namespaceOf(String plainElementId, String modelName) {
		int lastSeparator = plainElementId.lastIndexOf(':');
		return lastSeparator < 0
				? modelName
				: plainElementId.substring(0, lastSeparator);
	}

	/**
	 * Opens a namespace section with a banner comment, so the reader can find
	 * where one origin's steps end and the next begins.
	 */
	private void appendNamespaceBanner(String namespace) {
		String edge = "###" + " ".repeat(namespace.length()) + "###";
		builder.append(edge).append("\n");
		builder.append("## ").append(namespace).append(" ##\n");
		builder.append(edge).append("\n\n");
	}

	private void appendHeader(String sourcePath) {
		builder.append("# Generated by jPipe\n");
		builder.append("# Source : ").append(sourcePath).append("\n");
		builder.append("# Date   : ").append(Instant.now()).append("\n");
		builder.append("\n");
	}

	private void appendPreamble() {
		builder.append("from typing import Any, Callable\n");
		builder.append("from jpipe_runner.framework.decorators")
				.append(".jpipe_decorator import jpipe\n");
		builder.append("from jpipe_runner.framework.decorators")
				.append(".link_decorator import jpipe_link\n");
		builder.append("\n");
		builder.append("JpipeProduce = Callable[[str, Any], None]\n");
		builder.append("\n\n");
	}

	private void appendMethod(JustificationElement element) {
		List<String> links = linksFor(element);
		String name = LabelEscaper.toMethodName(element.label());
		String params = element instanceof Conclusion
				? ""
				: "produce: JpipeProduce";

		StringBuilder block = new StringBuilder();
		for (String link : links) {
			claim(link, name);
			block.append("@jpipe_link(\"").append(link).append("\")\n");
		}
		block.append("@jpipe(").append(jpipeDecoratorArgs(element))
				.append(")\n");
		block.append("def ").append(name).append("(").append(params)
				.append(") -> bool:\n");
		block.append("    \"\"\"[").append(element.kind()).append("] ")
				.append(escapeDocstring(element.label())).append("\"\"\"\n");
		if (element instanceof AbstractSupport) {
			block.append("    raise NotImplementedError(\"")
					.append(links.get(0)).append(" is abstract and must be"
							+ " overridden before execution\")\n");
		} else {
			block.append("    pass\n");
		}

		appendBlock(block.toString(), element);
		builder.append("\n\n");
	}

	/**
	 * Writes a function, commenting it out when the runner does not need it
	 * implemented.
	 *
	 * <p>
	 * Only evidence and strategies must be bound to run. A sub-conclusion
	 * executes solely when it happens to be bound, so leaving it live would add
	 * a function that never runs, while leaving it out would drop the claim it
	 * states from the module. Emitting it commented keeps the shape of the
	 * argument visible and lets a developer turn the check on by deleting the
	 * prefixes.
	 */
	private void appendBlock(String block, JustificationElement element) {
		if (!(element instanceof SubConclusion)) {
			builder.append(block);
			return;
		}
		builder.append(COMMENT).append("The runner passes this claim through"
				+ " unless it is bound. Uncomment to check it.\n");
		block.lines().forEach(
				line -> builder.append(COMMENT).append(line).append("\n"));
	}

	/**
	 * The ids this element should be linkable by: the ids it was merged from
	 * when it is a merge product, its own id otherwise, each shortened to the
	 * least qualified form that still designates it.
	 *
	 * <p>
	 * A merge product is named by what it was merged from rather than by its
	 * own id, which the operator chose ({@code hook}) or unification minted
	 * ({@code unified_0}). That own id stays the fallback for when no authored
	 * original can be resolved, so every function carries at least one link.
	 *
	 * <p>
	 * Two originals that shorten to the same form are emitted once, since the
	 * second decorator would say nothing the first does not. Source models that
	 * named an element identically do not collide this way: the shortened form
	 * keeps the model in front of the id, so {@code a:s} and {@code b:s} stay
	 * apart. The unshortened ids remain in the JSON export either way.
	 */
	private List<String> linksFor(JustificationElement element) {
		List<String> originals = qualifiedAuthoredOriginalsOf(element.id());
		List<String> keys = originals.isEmpty()
				? List.of(qualify(element.id()))
				: originals;
		return keys.stream().map(this::minimalLink).distinct().toList();
	}

	/**
	 * Records that {@code link} is emitted on {@code function}, and refuses to
	 * emit it a second time on a different one.
	 *
	 * <p>
	 * A consumer keys its binding registry by the link id, so an id appearing
	 * on two functions leaves it with two candidate implementations and no way
	 * to choose. Every id emitted here designates exactly one element by
	 * construction, because {@link AbstractModelExporter#minimalLink(String)}
	 * only shortens to a form that resolves uniquely, and falls back to a full
	 * id, which resolves exactly. Two functions can therefore collide only when
	 * a single identifier already designated two elements. That is the
	 * {@code unique-identifiers} consistency rule, which fails compilation
	 * before any export runs; this guard catches the same fault when the
	 * exporter is driven directly, rather than writing a module that cannot
	 * load.
	 */
	private void claim(String link, String function) {
		String existing = boundTo.putIfAbsent(link, function);
		if (existing != null && !existing.equals(function)) {
			throw new IllegalStateException("@jpipe_link(\"" + link
					+ "\") would be emitted on both '" + existing + "' and '"
					+ function + "' in model '" + currentModelName
					+ "': one identifier cannot designate two elements."
					+ " Check the model against the unique-identifiers rule.");
		}
	}

	private static String jpipeDecoratorArgs(JustificationElement element) {
		return switch (element) {
			case Conclusion _ -> "consume=[]";
			case Evidence _,AbstractSupport _ -> "produce=[]";
			case Strategy _,SubConclusion _ -> "produce=[], consume=[]";
		};
	}

	private static String escapeDocstring(String label) {
		return label.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
