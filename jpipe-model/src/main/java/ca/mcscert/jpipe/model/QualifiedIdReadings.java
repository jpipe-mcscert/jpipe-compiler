package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Reads a qualified id through the models it comes from (#202).
 *
 * <p>
 * An id is a string of {@code :}-separated segments, and a model loaded under a
 * namespace has a {@code :} in its name, so the text alone does not tell which
 * segments name a model: {@code b:ns:a:e} is {@code (b)(ns:a)(e)} or
 * {@code (b:ns)(a)(e)} depending on which models exist. A reading settles it by
 * provenance. In a composed model, an id is {@code source:rest}, where
 * {@code rest} is an id of that source. In a model that implements a template,
 * it is {@code template:rest} for an element the template declares, or the id
 * the template itself gives an element it inherits. The rest is then read in
 * that model, down to an id no model names: its local id.
 *
 * <p>
 * Model names are matched as a naming function gives them, so that an id can be
 * read as the unit writes it ({@link JustificationModel#getName}) or as a file
 * loaded under a namespace writes it, and then rewritten in another naming with
 * {@link Reading#with}.
 */
public final class QualifiedIdReadings {

	private static final String SEPARATOR = ":";

	private QualifiedIdReadings() {
	}

	/**
	 * One way to read an id: the models whose names it holds, outermost first,
	 * and the id left once they are read, which names no model.
	 *
	 * @param models
	 *            the models named in the id, in the order the id names them.
	 * @param local
	 *            what is left of the id after their names.
	 */
	public record Reading(List<JustificationModel> models, String local) {

		public Reading {
			models = List.copyOf(models);
		}

		/** The id this reading stands for, with each model named by names. */
		public String with(Function<JustificationModel, String> names) {
			return Stream.concat(models.stream().map(names), Stream.of(local))
					.reduce((a, b) -> a + SEPARATOR + b).orElse(local);
		}

		private Reading under(JustificationModel model) {
			List<JustificationModel> all = new ArrayList<>(models.size() + 1);
			all.add(model);
			all.addAll(models);
			return new Reading(all, local);
		}
	}

	/**
	 * The readings of {@code id}, an id of {@code model} written with model
	 * names given by {@code names}. A single reading with no model when no
	 * model's name can be read from the id: a plain id, an id minted by an
	 * operator, or an id that names nothing.
	 */
	public static List<Reading> in(JustificationModel model, String id,
			Function<JustificationModel, String> names) {
		Set<Reading> readings = new LinkedHashSet<>();
		model.composition().ifPresent(
				c -> readings.addAll(throughSources(c.sources(), id, names)));
		model.getParent().ifPresent(
				parent -> readings.addAll(throughParent(parent, id, names)));
		return readings.isEmpty()
				? List.of(new Reading(List.of(), id))
				: List.copyOf(readings);
	}

	/**
	 * The readings of {@code id}, an id given as an argument of an operator
	 * call on {@code sources}. Which model such an id belongs to is the
	 * operator's to say ({@code unifyExclude} names an element of the result,
	 * {@code source:id}; {@code refine}'s {@code hook} one of its first source,
	 * as that source names it), so it is read in every one: in the result, and
	 * in each source. Two readings that write the id the same way are the same
	 * for any caller that rewrites it.
	 */
	public static List<Reading> inCall(List<JustificationModel> sources,
			String id, Function<JustificationModel, String> names) {
		Set<Reading> readings = new LinkedHashSet<>(
				throughSources(sources, id, names));
		for (JustificationModel source : sources) {
			in(source, id, names).stream()
					.filter(r -> !r.models().isEmpty() && holds(source,
							r.with(JustificationModel::getName)))
					.forEach(readings::add);
		}
		return readings.isEmpty()
				? List.of(new Reading(List.of(), id))
				: List.copyOf(readings);
	}

	/**
	 * {@code source:rest}, for a source of a composition, where {@code rest} is
	 * an id the source holds.
	 */
	private static List<Reading> throughSources(
			List<JustificationModel> sources, String id,
			Function<JustificationModel, String> names) {
		List<Reading> readings = new ArrayList<>();
		for (JustificationModel source : sources) {
			restAfter(names.apply(source), id)
					.ifPresent(rest -> in(source, rest, names).stream()
							.filter(r -> holds(source,
									r.with(JustificationModel::getName)))
							.map(r -> r.under(source)).forEach(readings::add));
		}
		return readings;
	}

	/**
	 * {@code parent:rest}, for an element the parent declares with the plain id
	 * {@code rest}, or an id the parent holds as is: one it inherited, which
	 * template expansion does not qualify twice.
	 */
	private static List<Reading> throughParent(JustificationModel parent,
			String id, Function<JustificationModel, String> names) {
		List<Reading> readings = new ArrayList<>();
		restAfter(names.apply(parent), id).filter(
				rest -> !rest.contains(SEPARATOR) && holds(parent, rest))
				.ifPresent(rest -> readings
						.add(new Reading(List.of(parent), rest)));
		if (id.contains(SEPARATOR)) {
			in(parent, id, names).stream()
					.filter(r -> !r.models().isEmpty() && holds(parent,
							r.with(JustificationModel::getName)))
					.forEach(readings::add);
		}
		return readings;
	}

	private static Optional<String> restAfter(String name, String id) {
		String prefix = name + SEPARATOR;
		return id.startsWith(prefix) && id.length() > prefix.length()
				? Optional.of(id.substring(prefix.length()))
				: Optional.empty();
	}

	/**
	 * Whether {@code model} holds {@code id}: as the id of one of its elements,
	 * or as an id one was merged from.
	 */
	private static boolean holds(JustificationModel model, String id) {
		return model.aliases().containsKey(id) || model.allElements().stream()
				.map(JustificationElement::id).anyMatch(id::equals);
	}
}
