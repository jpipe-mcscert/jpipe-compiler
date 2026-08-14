package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.Justification;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Template;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base for exporters that serialise a single
 * {@link JustificationModel} to text. Provides the common infrastructure shared
 * by all single-model exporters:
 *
 * <ul>
 * <li>A {@link StringBuilder} accumulator ({@link #builder}) and the associated
 * reset-before-export pattern.
 * <li>A {@link #currentModelName} field populated from the model under export.
 * <li>A {@link #qualify(String)} helper that prefixes element ids with the
 * current model name ({@code "modelName:elementId"}).
 * <li>{@link #visit(Unit)} — rejects {@link Unit} with
 * {@link UnsupportedOperationException}; single-model exporters require
 * {@code SelectModel} to extract a model first.
 * <li>{@link #visit(Justification)} and {@link #visit(Template)} — both
 * delegate to {@link #exportModel(JustificationModel)}.
 * </ul>
 *
 * <p>
 * Subclasses implement {@link #exportModel(JustificationModel)} to perform the
 * actual serialisation, setting {@link #currentModelName} as their first
 * action. Element visit methods ({@code visit(Conclusion)}, etc.) are left
 * abstract so each exporter controls its own output format.
 */
public abstract class AbstractModelExporter
		implements
			JustificationVisitor<Void> {

	/**
	 * Fewest colon-separated segments a shortened identifier may keep, so that
	 * it always reads as {@code container:id}. See
	 * {@link #minimalLink(String)}.
	 */
	private static final int MIN_SEGMENTS = 2;

	/** Accumulates the serialised output. Reset at the start of each export. */
	protected final StringBuilder builder = new StringBuilder();

	/**
	 * Name of the model currently being exported. Set by
	 * {@link #exportModel(JustificationModel)} before any element visit methods
	 * are called.
	 */
	protected String currentModelName;

	/**
	 * Inverted alias map for the model currently being exported: merged element
	 * id &rarr; the list of original ids that were merged into it. Populated by
	 * {@link #initAliases(JustificationModel)}.
	 */
	private final Map<String, List<String>> aliasesByTarget = new HashMap<>();

	/**
	 * The identifiers a consumer of the exported model may use to designate an
	 * element: every element's qualified id, plus every qualified id it was
	 * merged from. Maps each such key to the plain id of the element it
	 * designates. Populated by {@link #initAliases(JustificationModel)} and
	 * used by {@link #minimalLink(String)} to decide how far a link can be
	 * shortened.
	 */
	private final Map<String, String> linkIndex = new LinkedHashMap<>();

	/**
	 * Ids unification minted within the model being exported. Populated by
	 * {@link #initAliases(JustificationModel)}; see
	 * {@link JustificationModel#recordUnifiedId(String)}.
	 */
	private Set<String> unifiedIds = Set.of();

	/**
	 * Qualifies {@code elementId} with the current model name:
	 * {@code "currentModelName:elementId"}.
	 */
	protected final String qualify(String elementId) {
		return currentModelName + ":" + elementId;
	}

	/**
	 * Builds the inverted alias lookup and the link index for {@code model}.
	 * Subclasses that surface aliases must call this at the start of
	 * {@link #exportModel(JustificationModel)}, after setting
	 * {@link #currentModelName}.
	 */
	protected final void initAliases(JustificationModel<?> model) {
		aliasesByTarget.clear();
		model.aliases().forEach((oldId, newId) -> aliasesByTarget
				.computeIfAbsent(newId, _ -> new ArrayList<>()).add(oldId));
		unifiedIds = model.unifiedIds();
		linkIndex.clear();
		model.conclusion().ifPresent(this::indexElement);
		model.getElements().forEach(this::indexElement);
	}

	private void indexElement(JustificationElement element) {
		linkIndex.put(qualify(element.id()), element.id());
		for (String original : qualifiedOriginalsOf(element.id())) {
			linkIndex.put(original, element.id());
		}
	}

	/**
	 * Returns the qualified original ids that were merged into
	 * {@code plainElementId}, or an empty list when the element is not the
	 * target of any alias. Requires {@link #initAliases(JustificationModel)} to
	 * have been called first.
	 */
	protected final List<String> qualifiedAliasesOf(String plainElementId) {
		List<String> originals = aliasesByTarget.get(plainElementId);
		if (originals == null) {
			return List.of();
		}
		return originals.stream().map(this::qualify).toList();
	}

	/**
	 * Returns every qualified id {@code plainElementId} was merged from,
	 * following alias chains transitively.
	 *
	 * <p>
	 * A composition applied to the result of another composition records a
	 * chain ({@code a -> merged -> unified_0}) rather than a flat mapping, so a
	 * one-hop lookup ({@link #qualifiedAliasesOf(String)}) stops at the
	 * intermediate and loses the id the user actually wrote. The returned list
	 * holds both the intermediates and the leaves, nearest first.
	 */
	protected final List<String> qualifiedOriginalsOf(String plainElementId) {
		return collectOriginals(plainElementId, false);
	}

	/**
	 * Returns the qualified ids {@code plainElementId} was merged from that an
	 * author actually wrote: the ids as they stood in the source models. Empty
	 * when the element is not a merge product.
	 *
	 * <p>
	 * Two kinds of id are passed over. One that was merged from further ids is
	 * skipped in favour of those, which stand closer to the source. One that
	 * unification minted ({@code unified_0}) is skipped outright: it names a
	 * merged group by a counter and appears in no source file. The latter is
	 * not always the former, since composing the result of a composition can
	 * leave a minted id recorded as a sibling of the ids it stood for rather
	 * than as their parent, so being childless does not make an id authored.
	 */
	protected final List<String> qualifiedAuthoredOriginalsOf(
			String plainElementId) {
		return collectOriginals(plainElementId, true);
	}

	private List<String> collectOriginals(String plainElementId,
			boolean authoredOnly) {
		if (!aliasesByTarget.containsKey(plainElementId)) {
			return List.of();
		}
		Set<String> found = new LinkedHashSet<>();
		Set<String> visited = new HashSet<>();
		visited.add(plainElementId);
		collectFrom(plainElementId, authoredOnly, visited, found);
		return found.stream().map(this::qualify).toList();
	}

	private void collectFrom(String id, boolean authoredOnly,
			Set<String> visited, Set<String> found) {
		for (String original : aliasesByTarget.getOrDefault(id, List.of())) {
			if (!visited.add(original)) {
				continue;
			}
			if (!authoredOnly || isAuthored(original)) {
				found.add(original);
			}
			collectFrom(original, authoredOnly, visited, found);
		}
	}

	private boolean isAuthored(String plainId) {
		return !aliasesByTarget.containsKey(plainId)
				&& !unifiedIds.contains(plainId);
	}

	/**
	 * Shortens {@code qualifiedKey} to the shortest trailing run of
	 * colon-separated segments that still designates the same element, never
	 * going below {@code container:id}.
	 *
	 * <p>
	 * Consumers resolve a link by exact match against the link index first,
	 * then by strict segment-suffix match; a suffix designating two different
	 * elements is ambiguous and is not a valid link. Candidates are therefore
	 * checked against the very index a consumer builds from the exported model,
	 * shortest first, and {@code qualifiedKey} itself is returned when no
	 * shorter form is unambiguous, because a full id always resolves exactly.
	 *
	 * <p>
	 * The {@value #MIN_SEGMENTS}-segment floor is a readability requirement
	 * rather than a resolution one: a bare {@code id} would often resolve, but
	 * a reader of the exported artefact has no way to tell what it belongs to.
	 * Keeping the owning model or source justification in front of it, as
	 * {@code a_claim:s1} rather than {@code s1}, makes the reference legible on
	 * its own. Every key starts qualified with the model name, so the floor is
	 * always reachable.
	 */
	protected final String minimalLink(String qualifiedKey) {
		String canonical = linkIndex.get(qualifiedKey);
		if (canonical == null) {
			return qualifiedKey;
		}
		String[] segments = qualifiedKey.split(":");
		for (int length = MIN_SEGMENTS; length < segments.length; length++) {
			String candidate = String.join(":", Arrays.copyOfRange(segments,
					segments.length - length, segments.length));
			if (canonical.equals(designatedBy(candidate))) {
				return candidate;
			}
		}
		return qualifiedKey;
	}

	/**
	 * Returns the plain id of the single element {@code candidate} designates,
	 * or {@code null} when it designates none or more than one.
	 */
	private String designatedBy(String candidate) {
		String exact = linkIndex.get(candidate);
		if (exact != null) {
			return exact;
		}
		String[] wanted = candidate.split(":");
		String found = null;
		for (Map.Entry<String, String> entry : linkIndex.entrySet()) {
			String[] segments = entry.getKey().split(":");
			if (!isStrictSuffix(wanted, segments)) {
				continue;
			}
			if (found != null && !found.equals(entry.getValue())) {
				return null;
			}
			found = entry.getValue();
		}
		return found;
	}

	/**
	 * Tells whether {@code wanted} is a strictly shorter tail of {@code of}.
	 */
	private static boolean isStrictSuffix(String[] wanted, String[] of) {
		if (wanted.length >= of.length) {
			return false;
		}
		return Arrays.equals(of, of.length - wanted.length, of.length, wanted,
				0, wanted.length);
	}

	/**
	 * Rejects {@link Unit} — single-model exporters require a specific model.
	 * Use {@code SelectModel} in the compilation pipeline to extract one first.
	 */
	@Override
	public final Void visit(Unit unit) {
		throw new UnsupportedOperationException(
				getClass().getSimpleName() + " operates on a single model"
						+ " — use SelectModel to extract one from a Unit");
	}

	/** Delegates to {@link #exportModel(JustificationModel)}. */
	@Override
	public final Void visit(Justification justification) {
		exportModel(justification);
		return null;
	}

	/** Delegates to {@link #exportModel(JustificationModel)}. */
	@Override
	public final Void visit(Template template) {
		exportModel(template);
		return null;
	}

	/**
	 * Performs the actual serialisation of {@code model}. Implementations must
	 * set {@link #currentModelName}{@code = model.getName()} before calling any
	 * element visit methods.
	 */
	protected abstract void exportModel(JustificationModel<?> model);
}
