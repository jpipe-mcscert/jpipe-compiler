package ca.mcscert.jpipe.visitor;

import static ca.mcscert.jpipe.util.QualifiedIds.qualifyIfPlain;

import ca.mcscert.jpipe.model.Composition;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.util.QualifiedIds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The elements a composed model was built from, at every level of composition
 * (#200).
 *
 * <p>
 * A composition copies each element {@code x} of a source {@code S} as
 * {@code S:x}, and the ids it merges are recorded as aliases, the merged id
 * standing for them. The element {@code S:x} became is therefore {@code S:x}
 * followed through the composed model's aliases, which is how the composition
 * itself rewires the support edges, whatever the operator. Each source element
 * found that way is an origin of the element, with the kind it has in its
 * source, and so are its own origins in that source, if the source was composed
 * too.
 */
final class CompositionOrigins {

	/**
	 * An element a composed model's element was built from.
	 *
	 * @param model
	 *            the name of the model the element belongs to.
	 * @param id
	 *            its id, as the JSON export of that model writes it.
	 * @param type
	 *            its kind there, as the JSON export writes it.
	 * @param level
	 *            1 for an element of a source, 2 for an element of a source of
	 *            a source, and so on.
	 */
	record Origin(String model, String id, String type, int level) {
	}

	private CompositionOrigins() {
	}

	/**
	 * The origins of each element of {@code model}, keyed by its id, nearest
	 * level first, and within a level in the order of the call's sources and of
	 * their elements. Empty for a model no operator built and that inherits
	 * from no template an operator built, and with no entry for an element the
	 * operator created itself.
	 */
	static Map<String, List<Origin>> of(JustificationModel model) {
		return of(model, new IdentityHashMap<>());
	}

	/**
	 * The origins of {@code model}, each model's computed once: a model a
	 * composition reaches by several paths is walked once.
	 */
	private static Map<String, List<Origin>> of(JustificationModel model,
			Map<JustificationModel, Map<String, List<Origin>>> known) {
		Map<String, List<Origin>> cached = known.get(model);
		if (cached != null) {
			return cached;
		}
		Map<String, List<Origin>> origins = new LinkedHashMap<>();
		model.composition().ifPresent(
				composition -> collect(model, composition, origins, known));
		model.getParent()
				.ifPresent(parent -> inherit(model, parent, origins, known));
		origins.replaceAll((_, list) -> nearestOnce(list));
		known.put(model, origins);
		return origins;
	}

	/**
	 * Gives each element {@code model} inherits from {@code parent} unchanged
	 * the origins it has there: a template built by an operator passes them on
	 * to the models that implement it. An element that fills a placeholder is
	 * the model's own, and has none.
	 */
	private static void inherit(JustificationModel model,
			JustificationModel parent, Map<String, List<Origin>> origins,
			Map<JustificationModel, Map<String, List<Origin>>> known) {
		Set<String> inherited = new HashSet<>();
		model.inheritedElements().forEach(e -> inherited.add(e.id()));
		if (!model.hasOwnConclusion()) {
			model.conclusion().ifPresent(c -> inherited.add(c.id()));
		}
		of(parent, known).forEach((id, list) -> {
			String copy = qualifyIfPlain(parent.getName(), id);
			if (inherited.contains(copy)) {
				origins.computeIfAbsent(copy, _ -> new ArrayList<>())
						.addAll(list);
			}
		});
	}

	/**
	 * {@code origins} nearest level first, each origin once, at the nearest
	 * level it was found: a model reached by several paths, as a diamond whose
	 * branches are merged again, is found once per path.
	 */
	private static List<Origin> nearestOnce(List<Origin> origins) {
		Set<List<String>> seen = new HashSet<>();
		List<Origin> once = new ArrayList<>();
		origins.stream().sorted(Comparator.comparingInt(Origin::level))
				.filter(o -> seen.add(List.of(o.model(), o.id(), o.type())))
				.forEach(once::add);
		return once;
	}

	private static void collect(JustificationModel model,
			Composition composition, Map<String, List<Origin>> origins,
			Map<JustificationModel, Map<String, List<Origin>>> known) {
		for (JustificationModel source : composition.sources()) {
			Map<String, List<Origin>> deeper = of(source, known);
			for (JustificationElement element : source.allElements()) {
				String copy = QualifiedIds.qualify(source.getName(),
						element.id());
				List<Origin> list = origins.computeIfAbsent(
						resolve(model.aliases(), copy), _ -> new ArrayList<>());
				list.add(new Origin(source.getName(), copy, element.kind(), 1));
				deeper.getOrDefault(element.id(), List.of())
						.forEach(o -> list.add(new Origin(o.model(), o.id(),
								o.type(), o.level() + 1)));
			}
		}
	}

	/** {@code id} followed through {@code aliases} to the end of the chain. */
	private static String resolve(Map<String, String> aliases, String id) {
		String current = id;
		Set<String> visited = new HashSet<>();
		while (aliases.containsKey(current) && visited.add(current)) {
			current = aliases.get(current);
		}
		return current;
	}
}
