package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads a qualified id an author wrote, an argument of an operator call,
 * through the models it comes from (#202). The ids the compiler holds are never
 * read this way: each model records where its copies come from (see
 * {@link JustificationModel#originOf}, #209). An argument is text, and is read
 * against the models that exist.
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
					.collect(Collectors.joining(SEPARATOR));
		}

		private Reading under(JustificationModel model) {
			List<JustificationModel> all = new ArrayList<>(models.size() + 1);
			all.add(model);
			all.addAll(models);
			return new Reading(all, local);
		}
	}

	/**
	 * The readings of {@code id}, an argument of an operator call on
	 * {@code sources} that names an element of the result, such as
	 * {@code unifyExclude}: {@code source:rest}, where {@code rest} is an id of
	 * that source.
	 */
	public static List<Reading> inResult(List<JustificationModel> sources,
			String id, Function<JustificationModel, String> names) {
		return orLocal(new Reader(names).throughSources(sources, id), id);
	}

	/**
	 * A reading of an id in one of the sources of a call.
	 *
	 * @param source
	 *            the source the id was read in.
	 * @param reading
	 *            the reading, of an id of that source.
	 */
	public record SourceReading(JustificationModel source, Reading reading) {
	}

	/**
	 * The readings of {@code id}, an argument of an operator call on
	 * {@code sources} that names an element of a source as that source names
	 * it, such as {@code refine}'s {@code hook}. It is read in every source,
	 * since which one is the operator's to say. A qualified id no source reads
	 * stands for the elements whose id ends with it, the short form
	 * {@link JustificationModel#findById} accepts: those elements are read
	 * instead. Empty when the id names no model: a plain id, or an id that
	 * names nothing.
	 */
	public static List<SourceReading> inSources(
			List<JustificationModel> sources, String id,
			Function<JustificationModel, String> names) {
		Reader reader = new Reader(names);
		List<SourceReading> readings = new ArrayList<>();
		for (JustificationModel source : sources) {
			reader.in(source, id).stream().filter(reader.heldBy(source))
					.forEach(r -> readings.add(new SourceReading(source, r)));
		}
		if (readings.isEmpty() && id.contains(SEPARATOR)) {
			sources.forEach(
					source -> readings.addAll(endingWith(source, id, names)));
		}
		return List.copyOf(readings);
	}

	/**
	 * The readings of the elements of {@code source} whose id, written with
	 * {@code names}, is {@code id} or ends with {@code :id}.
	 */
	private static List<SourceReading> endingWith(JustificationModel source,
			String id, Function<JustificationModel, String> names) {
		Reader unit = new Reader(JustificationModel::getName);
		List<SourceReading> readings = new ArrayList<>();
		for (JustificationElement element : source.allElements()) {
			orLocal(unit.in(source, element.id()), element.id()).stream()
					.filter(r -> {
						String written = r.with(names);
						return written.equals(id)
								|| written.endsWith(SEPARATOR + id);
					}).map(r -> new SourceReading(source, r))
					.forEach(readings::add);
		}
		return readings;
	}

	/**
	 * The ids {@code readings} stand for, each model named by {@code names},
	 * each once, in the order of the readings. More than one when the id they
	 * read names several elements.
	 */
	public static List<String> rewritten(List<Reading> readings,
			Function<JustificationModel, String> names) {
		return readings.stream().map(r -> r.with(names)).distinct().toList();
	}

	private static List<Reading> orLocal(Collection<Reading> readings,
			String id) {
		return readings.isEmpty()
				? List.of(new Reading(List.of(), id))
				: List.copyOf(readings);
	}

	/**
	 * Reads ids with one naming, and remembers the ids each model holds, so
	 * that reading through a chain does not list a model's elements again at
	 * every step.
	 */
	private static final class Reader {

		private final Function<JustificationModel, String> names;
		private final Map<JustificationModel, Set<String>> held = new IdentityHashMap<>();

		Reader(Function<JustificationModel, String> names) {
			this.names = names;
		}

		/** The readings of {@code id} in {@code model}, none if it is local. */
		Set<Reading> in(JustificationModel model, String id) {
			Set<Reading> readings = new LinkedHashSet<>();
			model.composition().ifPresent(
					c -> readings.addAll(throughSources(c.sources(), id)));
			model.getParent().ifPresent(
					parent -> readings.addAll(throughParent(parent, id)));
			return readings;
		}

		/**
		 * {@code source:rest}, for a source of a composition, where
		 * {@code rest} is an id the source holds.
		 */
		List<Reading> throughSources(List<JustificationModel> sources,
				String id) {
			List<Reading> readings = new ArrayList<>();
			for (JustificationModel source : sources) {
				restAfter(names.apply(source), id).ifPresent(rest -> {
					Set<Reading> inner = in(source, rest);
					Stream<Reading> candidates = inner.isEmpty()
							? Stream.of(new Reading(List.of(), rest))
							: inner.stream();
					candidates.filter(r -> holds(source, r))
							.map(r -> r.under(source)).forEach(readings::add);
				});
			}
			return readings;
		}

		/**
		 * {@code parent:rest}, for an element the parent declares with the
		 * plain id {@code rest}, or an id the parent holds as is: one it
		 * inherited, which template expansion does not qualify twice.
		 */
		private List<Reading> throughParent(JustificationModel parent,
				String id) {
			List<Reading> readings = new ArrayList<>();
			restAfter(names.apply(parent), id).filter(
					rest -> !rest.contains(SEPARATOR) && holds(parent, rest))
					.ifPresent(rest -> readings
							.add(new Reading(List.of(parent), rest)));
			if (id.contains(SEPARATOR)) {
				in(parent, id).stream().filter(heldBy(parent))
						.forEach(readings::add);
			}
			return readings;
		}

		/**
		 * Whether a reading names models, and stands for an id {@code model}
		 * holds.
		 */
		Predicate<Reading> heldBy(JustificationModel model) {
			return r -> !r.models().isEmpty() && holds(model, r);
		}

		/**
		 * Whether {@code model} holds the id {@code reading} stands for, as the
		 * unit names it: as the id of one of its elements, or as an id one was
		 * merged from.
		 */
		private boolean holds(JustificationModel model, Reading reading) {
			return holds(model, reading.with(JustificationModel::getName));
		}

		private boolean holds(JustificationModel model, String id) {
			return held.computeIfAbsent(model, Reader::idsOf).contains(id);
		}

		/** What follows {@code name:} in {@code id}, if it starts with it. */
		private static Optional<String> restAfter(String name, String id) {
			String prefix = name + SEPARATOR;
			return id.startsWith(prefix) && id.length() > prefix.length()
					? Optional.of(id.substring(prefix.length()))
					: Optional.empty();
		}

		private static Set<String> idsOf(JustificationModel model) {
			Set<String> ids = new HashSet<>(model.aliases().keySet());
			model.allElements().forEach(e -> ids.add(e.id()));
			return ids;
		}
	}
}
