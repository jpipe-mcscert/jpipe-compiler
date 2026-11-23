package ca.mcscert.jpipe.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
 * to free text such as a label, each with the {@link Scope} its ids are read
 * in. The arguments are kept sorted by key, so that writing the call back is
 * deterministic.
 *
 * @param operator
 *            the name of the operator, as the call wrote it.
 * @param sources
 *            the models the operator composed, in call order.
 * @param arguments
 *            the call's arguments, keyed by name.
 * @param idArguments
 *            the keys of {@code arguments} whose value holds element ids, and
 *            where those ids name an element.
 */
public record Composition(String operator, List<JustificationModel> sources,
		Map<String, String> arguments, Map<String, Scope> idArguments) {

	/**
	 * Which element an id argument of a call names: one of the result, written
	 * {@code source:id} ({@code unifyExclude}), or one of a source, written as
	 * that source names it ({@code refine}'s {@code hook}).
	 */
	public enum Scope {
		RESULT, SOURCE
	}

	public Composition {
		sources = List.copyOf(sources);
		arguments = Collections.unmodifiableMap(new TreeMap<>(arguments));
		idArguments = Map.copyOf(idArguments);
	}
}
