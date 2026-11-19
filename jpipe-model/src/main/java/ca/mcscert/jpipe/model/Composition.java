package ca.mcscert.jpipe.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The operator call a composed model was built by: the operator, the models it
 * composed, in call order, and the arguments it was given.
 *
 * <p>
 * The arguments hold element ids as the unit names them: a call read from a
 * file loaded under a namespace has them mapped into that namespace, so that
 * the call can be written back outside it. {@code idArguments} names the keys
 * whose value is an element id, or a comma-separated list of them, as opposed
 * to free text such as a label. Of those, {@code resultIdArguments} names the
 * ones whose ids name an element of the result, {@code source:id}, such as
 * {@code unifyExclude}; the others name an element of a source, as that source
 * names it, such as {@code refine}'s {@code hook}. The arguments are kept
 * sorted by key, so that writing the call back is deterministic.
 *
 * @param operator
 *            the name of the operator, as the call wrote it.
 * @param sources
 *            the models the operator composed, in call order.
 * @param arguments
 *            the call's arguments, keyed by name.
 * @param idArguments
 *            the keys of {@code arguments} whose value holds element ids.
 * @param resultIdArguments
 *            the keys of {@code idArguments} whose ids name an element of the
 *            result.
 */
public record Composition(String operator, List<JustificationModel> sources,
		Map<String, String> arguments, Set<String> idArguments,
		Set<String> resultIdArguments) {

	public Composition {
		sources = List.copyOf(sources);
		arguments = Collections.unmodifiableMap(new TreeMap<>(arguments));
		idArguments = Set.copyOf(idArguments);
		resultIdArguments = Set.copyOf(resultIdArguments);
	}

	/**
	 * A call whose id arguments all name an element of a source, as that source
	 * names it.
	 */
	public Composition(String operator, List<JustificationModel> sources,
			Map<String, String> arguments, Set<String> idArguments) {
		this(operator, sources, arguments, idArguments, Set.of());
	}
}
