package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.util.LabelEscaper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;

/**
 * Serialises a single {@link JustificationModel} to a step library for <a href=
 * "https://github.com/jpipe-mcscert/jpipe-runner/blob/main/docs/authoring.md">jpipe-runner
 * 4</a>: each element becomes a function named after the element's label
 * (snake_case via {@link LabelEscaper}, qualified where two would collide; see
 * {@link #nameFunctions(List)}), returning an {@code Outcome}, under the one
 * decorator its kind takes ({@code @evidence}, {@code @strategy},
 * {@code @sub_conclusion} or {@code @conclusion}). The decorator carries:
 *
 * <ul>
 * <li>as positional arguments, the ids binding the function to the originating
 * jPipe element. An element that resulted from a merge is bound by the ids it
 * was merged from rather than by its own, transitively, so a composition of a
 * composition still names the ids as the original source models had them; ids
 * unification minted along the way ({@code unified_0}) are left out, having
 * been written in no source file. Every id is then shortened to the least
 * qualified form that still designates the element unambiguously, but never
 * past {@code container:id}, because a bare id would leave a reader of the
 * generated module unable to tell what it refers to; see
 * {@link AbstractModelExporter#minimalLink(String)}.</li>
 * <li>as keyword arguments, empty {@code observes}, {@code consumes} and
 * {@code produces} declarations for the developer to fill in. The exporter
 * cannot know what an evidence reads or what a step produces, and the runner
 * refuses evidence that observes or produces nothing, so its validation of the
 * generated module lists what is left to write.</li>
 * </ul>
 *
 * <p>
 * Each function carries a docstring with the original element label so the full
 * text is always recoverable regardless of escaping, and raises
 * {@code NotImplementedError} until it is written. A label too long for one
 * line is wrapped between words, a line break standing for the space it
 * replaces.
 *
 * <p>
 * The module is hard-wrapped at {@value #LINE_WIDTH} columns, commented-out
 * functions included, so that it passes a line-length check as generated. What
 * has no place to break is left whole: a function name, a single word of a
 * label, an id, or the source path in the header.
 *
 * <p>
 * This visitor operates on one model at a time. Use {@code SelectModel} in the
 * compilation pipeline to extract the desired model from a {@link Unit} before
 * invoking this exporter.
 */
public class PythonExporter extends AbstractModelExporter {

	/** Prefix that comments out a line of generated Python. */
	private static final String COMMENT = "# ";

	/** Where the step-library API the generated module targets is described. */
	static final String AUTHORING_GUIDE = "https://github.com/jpipe-mcscert/"
			+ "jpipe-runner/blob/main/docs/authoring.md";

	/** Closing line of the note above a function emitted commented out. */
	private static final String UNCOMMENT = "Uncomment to check it.";

	/** Longest line the generated module holds, where a line can be broken. */
	static final int LINE_WIDTH = 80;

	/** One level of Python indentation. */
	private static final String INDENT = "    ";

	/** Opens and closes a docstring. */
	private static final String DOCSTRING_QUOTES = "\"\"\"";

	/** The decorator of an evidence, the one step that observes artifacts. */
	private static final String EVIDENCE = "evidence";

	/**
	 * The decorators the module being written uses, commented-out functions
	 * included, so that the preamble imports exactly those and uncommenting a
	 * function needs no import added by hand.
	 */
	private final Set<String> decorators = new TreeSet<>();

	/**
	 * Link id &rarr; the function it has already been emitted on, for the
	 * module being written. Consumers key their registry by the link id, so the
	 * same id on two functions has no resolution; see
	 * {@link #claim(String, String)}.
	 */
	private final Map<String, String> boundTo = new LinkedHashMap<>();

	/**
	 * Names a generated function cannot take: the decorators the preamble
	 * imports, which a function of the same name would replace for every step
	 * declared below it, and Python's keywords, which are no function name at
	 * all. Names are lower case, so only lower-case keywords can arise.
	 */
	private static final Set<String> RESERVED_NAMES = Set.of("conclusion",
			EVIDENCE, "strategy", "sub_conclusion", "and", "as", "assert",
			"async", "await", "break", "class", "continue", "def", "del",
			"elif", "else", "except", "finally", "for", "from", "global", "if",
			"import", "in", "is", "lambda", "nonlocal", "not", "or", "pass",
			"raise", "return", "try", "while", "with", "yield");

	/**
	 * Element &rarr; the name of its function, for the module being written;
	 * see {@link #nameFunctions(List)}. Keyed by the element itself, not its
	 * id: two elements sharing an id must still get two functions, so that
	 * {@link #claim(String, String)} sees the id bound twice and refuses it.
	 */
	private final Map<JustificationElement, String> functionNames = new IdentityHashMap<>();

	/**
	 * Serialise {@code model} to Python source text without a file header.
	 *
	 * @param model
	 *            the justification or template to serialise.
	 * @return Python source text.
	 */
	public String export(JustificationModel model) {
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
	public String export(JustificationModel model, String sourcePath) {
		builder.setLength(0);
		decorators.clear();
		model.accept(this);
		// The preamble imports the decorators the body uses, so it is known
		// only once the body is written.
		String body = builder.toString();
		builder.setLength(0);
		if (sourcePath != null) {
			appendHeader(sourcePath);
		}
		appendPreamble();
		builder.append(body);
		return builder.toString();
	}

	// -------------------------------------------------------------------------
	// Element visit methods
	// -------------------------------------------------------------------------

	@Override
	public Void visit(Conclusion conclusion) {
		appendMethod(conclusion);
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
	protected void exportModel(JustificationModel model) {
		currentModelName = model.getName();
		initAliases(model);
		boundTo.clear();

		Map<String, List<JustificationElement>> byNamespace = new TreeMap<>();
		model.getElements()
				.forEach(e -> group(byNamespace, e, currentModelName));
		model.conclusion()
				.ifPresent(c -> group(byNamespace, c, currentModelName));
		nameFunctions(
				byNamespace.values().stream().flatMap(List::stream).toList());

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

	/**
	 * Gives every function in the module a name no other function takes.
	 *
	 * <p>
	 * A function is named after its element's label. Two elements can carry the
	 * same label, which is common once a composition brings several source
	 * models together, and two functions of one name are one function in
	 * Python: the second replaces the first, and the runner never sees the step
	 * it held. Functions whose names would collide, or whose name is reserved
	 * ({@link #RESERVED_NAMES}), are therefore named in a more qualified form,
	 * each tried in turn until it is free:
	 *
	 * <ol>
	 * <li>the label, alone;</li>
	 * <li>the label behind the element's namespace, which tells apart the same
	 * label in two source models ({@code a_claim_a_strategy});</li>
	 * <li>the label behind the element's first bound id, for two elements with
	 * one label in one namespace ({@code expected_minimal_s_a_strategy});</li>
	 * <li>that form with a counter, should escaping fold two ids into one
	 * name.</li>
	 * </ol>
	 *
	 * <p>
	 * Only the functions that collide are renamed, and every one of them is, so
	 * that neither reads as the original. Commented-out functions count, so
	 * that uncommenting one never clashes.
	 */
	private void nameFunctions(List<JustificationElement> elements) {
		functionNames.clear();
		Set<String> taken = new HashSet<>(RESERVED_NAMES);
		Function<JustificationElement, String> byId = e -> LabelEscaper
				.toMethodName(linksFor(e).getFirst() + " " + e.label());
		List<JustificationElement> pending = elements;
		pending = nameUnique(pending, e -> LabelEscaper.toMethodName(e.label()),
				taken);
		pending = nameUnique(pending, e -> LabelEscaper.toMethodName(
				namespaceOf(e.id(), currentModelName) + " " + e.label()),
				taken);
		pending = nameUnique(pending, byId, taken);
		for (JustificationElement element : pending) {
			String base = byId.apply(element);
			String name = base;
			for (int n = 2; !taken.add(name); n++) {
				name = base + "_" + n;
			}
			functionNames.put(element, name);
		}
	}

	/**
	 * Names, in the given form, each pending element whose name in that form is
	 * shared with no other pending element and not yet taken, and returns the
	 * elements left unnamed.
	 */
	private List<JustificationElement> nameUnique(
			List<JustificationElement> pending,
			Function<JustificationElement, String> form, Set<String> taken) {
		Map<String, List<JustificationElement>> byName = new LinkedHashMap<>();
		pending.forEach(e -> byName
				.computeIfAbsent(form.apply(e), _ -> new ArrayList<>()).add(e));
		List<JustificationElement> clashing = new ArrayList<>();
		byName.forEach((name, named) -> {
			if (named.size() == 1 && taken.add(name)) {
				functionNames.put(named.getFirst(), name);
			} else {
				clashing.addAll(named);
			}
		});
		return clashing;
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

	/**
	 * Names the runner the module is written for, so that a reader of a module
	 * generated by an older jPipe can tell which API it targets.
	 */
	private void appendHeader(String sourcePath) {
		builder.append("# Generated by jPipe\n");
		builder.append("# Source : ").append(sourcePath).append("\n");
		builder.append("# Step library for jpipe-runner 4:\n");
		builder.append(COMMENT).append(AUTHORING_GUIDE).append("\n");
		builder.append("\n");
	}

	/**
	 * Imports {@code Path}, the type the runner injects for each artifact an
	 * evidence observes, when the module holds an evidence to annotate with it;
	 * then, from the package root, the only path the runner keeps stable, the
	 * decorators the module uses and the outcomes a step returns. The standard
	 * library comes first, in its own group, as import sorters expect. The
	 * decorators and the outcomes take a line each, which keeps both within
	 * {@value #LINE_WIDTH} columns whichever decorators are used.
	 */
	private void appendPreamble() {
		if (decorators.contains(EVIDENCE)) {
			builder.append("from pathlib import Path\n\n");
		}
		String from = "from jpipe_runner import ";
		// A model with no element uses no decorator, and an import of
		// nothing does not parse.
		if (!decorators.isEmpty()) {
			builder.append(from).append(String.join(", ", decorators))
					.append("\n");
		}
		builder.append(from).append("Fail, Outcome, Pass\n\n");
	}

	private void appendMethod(JustificationElement element) {
		List<String> links = linksFor(element);
		String name = functionNames.get(element);
		String decorator = decoratorOf(element);
		decorators.add(decorator);
		Optional<String> note = commentedOutNote(element);
		// A commented-out line is longer by its prefix, which has to fit too.
		int width = LINE_WIDTH - (note.isPresent() ? COMMENT.length() : 0);

		List<String> arguments = new ArrayList<>();
		for (String link : links) {
			claim(link, name);
			arguments.add("\"" + link + "\"");
		}
		arguments.addAll(declarationsOf(element));

		StringBuilder block = new StringBuilder();
		block.append(call("@" + decorator, arguments, width));
		block.append("def ").append(name).append("() -> Outcome:\n");
		block.append(docstring(element, width));
		block.append(
				raise(links.getFirst() + notImplementedReason(element), width));

		if (note.isEmpty()) {
			builder.append(block);
		} else {
			builder.append(COMMENT).append(note.get()).append("\n");
			builder.append(COMMENT).append(UNCOMMENT).append("\n");
			block.toString().lines().forEach(
					line -> builder.append(COMMENT).append(line).append("\n"));
		}
		builder.append("\n\n");
	}

	/**
	 * A top-level call within {@code width} columns, broken the way Python
	 * formatters break one: on a single line when it fits; otherwise with its
	 * arguments together on a line of their own; otherwise with one argument a
	 * line, each followed by a comma.
	 */
	private static String call(String callee, List<String> arguments,
			int width) {
		String inline = String.join(", ", arguments);
		String oneLine = callee + "(" + inline + ")";
		if (oneLine.length() <= width) {
			return oneLine + "\n";
		}
		StringBuilder out = new StringBuilder(callee).append("(\n");
		if (INDENT.length() + inline.length() <= width) {
			out.append(INDENT).append(inline).append("\n");
		} else {
			arguments.forEach(a -> out.append(INDENT).append(a).append(",\n"));
		}
		return out.append(")\n").toString();
	}

	/**
	 * The statement a function body is made of until it is written, within
	 * {@code width} columns: on one line when it fits, otherwise with the
	 * message on lines of its own, as adjacent string literals when it takes
	 * more than one, which Python reads as a single string.
	 */
	private static String raise(String message, int width) {
		String statement = INDENT + "raise NotImplementedError(";
		String oneLine = statement + "\"" + message + "\")";
		if (oneLine.length() <= width) {
			return oneLine + "\n";
		}
		String inner = INDENT + INDENT;
		int room = width - inner.length() - 2;
		// A literal after the first opens with the space the break stood for.
		List<String> literals = wrap(message, room, room - 1);
		StringBuilder out = new StringBuilder(statement).append("\n");
		for (int i = 0; i < literals.size(); i++) {
			out.append(inner).append('"').append(i == 0 ? "" : " ")
					.append(literals.get(i)).append("\"\n");
		}
		return out.append(INDENT).append(")\n").toString();
	}

	/**
	 * The docstring of an element's function: its kind and label, on one line
	 * when they fit within {@code width} columns, otherwise wrapped between
	 * words with the closing quotes on a line of their own.
	 */
	private static String docstring(JustificationElement element, int width) {
		String text = "[" + element.kind() + "] "
				+ escapeDocstring(element.label());
		String oneLine = INDENT + DOCSTRING_QUOTES + text + DOCSTRING_QUOTES;
		if (oneLine.length() <= width) {
			return oneLine + "\n";
		}
		int room = width - INDENT.length();
		List<String> lines = wrap(text, room - DOCSTRING_QUOTES.length(), room);
		return INDENT + DOCSTRING_QUOTES + String.join("\n" + INDENT, lines)
				+ "\n" + INDENT + DOCSTRING_QUOTES + "\n";
	}

	/**
	 * Breaks {@code text} between words into lines of at most {@code first}
	 * characters for the first and {@code rest} for the others. A word longer
	 * than a line stays whole. Only the space a line is broken at is dropped:
	 * runs of spaces within a line are kept as written.
	 */
	private static List<String> wrap(String text, int first, int rest) {
		List<String> lines = new ArrayList<>();
		StringBuilder line = new StringBuilder();
		int room = first;
		for (String word : text.split(" ", -1)) {
			if (!line.isEmpty() && line.length() + 1 + word.length() > room) {
				lines.add(line.toString());
				line.setLength(0);
				room = rest;
			}
			if (!line.isEmpty()) {
				line.append(' ');
			}
			line.append(word);
		}
		lines.add(line.toString());
		return lines;
	}

	/**
	 * Why the generated function does not run yet. Raising, rather than
	 * returning, makes the runner fail the step with this message, which names
	 * the element; a body that returned nothing would be reported as a step
	 * that does not return an outcome.
	 */
	private static String notImplementedReason(JustificationElement element) {
		return element instanceof AbstractSupport
				? " is abstract and must be overridden before execution"
				: " is not implemented yet";
	}

	/**
	 * Why a function is emitted commented out, for the elements the runner does
	 * not need implemented; empty for those it does.
	 *
	 * <p>
	 * Only evidence and strategies must be bound to run. A conclusion or
	 * sub-conclusion takes its status from what supports it unless it is bound,
	 * in which case its step runs after everything below it. Leaving it live
	 * would make a developer implement a check the argument does not need,
	 * while leaving it out would drop the claim it states from the module.
	 * Emitting it commented keeps the shape of the argument visible and lets a
	 * developer turn the check on by deleting the prefixes.
	 */
	private static Optional<String> commentedOutNote(
			JustificationElement element) {
		return switch (element) {
			case Conclusion _ -> Optional.of("The runner concludes this claim"
					+ " from what supports it unless it is bound.");
			case SubConclusion _ -> Optional.of(
					"The runner passes this claim through unless it is bound.");
			default -> Optional.empty();
		};
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
	 * The runner binds functions to elements one to one, so an id appearing on
	 * two functions leaves it with two candidate implementations and no way to
	 * choose. Every id emitted here designates exactly one element by
	 * construction, because {@link AbstractModelExporter#minimalLink(String)}
	 * only shortens to a form that resolves uniquely, and falls back to a full
	 * id, which resolves exactly. Two functions can therefore collide only when
	 * a single identifier already designated two elements: two elements sharing
	 * an id, or an alias landing on another element's id. The
	 * {@code no-duplicate-ids} and {@code unique-identifiers} consistency rules
	 * report that as an error, but the export step still runs after them, as it
	 * does when the exporter is driven directly. This guard is what keeps it
	 * from writing a module in which one function silently replaces another.
	 */
	private void claim(String link, String function) {
		String existing = boundTo.putIfAbsent(link, function);
		if (existing != null && !existing.equals(function)) {
			throw new AmbiguousIdentifierException("The id \"" + link
					+ "\" would be bound to both '" + existing + "' and '"
					+ function + "' in model '" + currentModelName
					+ "': one identifier cannot designate two elements."
					+ " Check the model against the no-duplicate-ids and"
					+ " unique-identifiers rules.");
		}
	}

	/**
	 * The runner decorator for an element's kind. An abstract support is a
	 * placeholder for the evidence a justification implementing the template
	 * puts in its place, so it is written as one.
	 */
	private static String decoratorOf(JustificationElement element) {
		return switch (element) {
			case Evidence _,AbstractSupport _ -> EVIDENCE;
			case Strategy _ -> "strategy";
			case SubConclusion _ -> "sub_conclusion";
			case Conclusion _ -> "conclusion";
		};
	}

	/**
	 * The empty declarations a step of this kind takes, for the developer to
	 * fill in: evidence observes artifacts rather than consuming values, and a
	 * conclusion is terminal, producing nothing.
	 */
	private static List<String> declarationsOf(JustificationElement element) {
		return switch (element) {
			case Evidence _,AbstractSupport _ ->
				List.of("observes={}", "produces=[]");
			case Strategy _,SubConclusion _ ->
				List.of("consumes=[]", "produces=[]");
			case Conclusion _ -> List.of("consumes=[]");
		};
	}

	private static String escapeDocstring(String label) {
		return Quoting.escaped(label);
	}
}
