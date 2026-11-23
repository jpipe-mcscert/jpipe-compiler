package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.QualifiedIdReadings;
import ca.mcscert.jpipe.model.QualifiedIdReadings.Reading;
import ca.mcscert.jpipe.model.QualifiedIdReadings.SourceReading;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Names the JPIPE export gives the models it writes, so that every name is a
 * plain identifier the grammar accepts.
 *
 * <p>
 * A model loaded under a namespace is named {@code ns:model} in the unit, and
 * the grammar has no {@code :} in a model name. The export inlines such a model
 * rather than writing a {@code load} back, so it renames it: each {@code :}
 * becomes {@code _} ({@code base:t} becomes {@code base_t}), followed by
 * {@code _2}, {@code _3}, and so on when that name is already taken. Every id
 * that names the model is renamed with it: {@code base:t:abs} becomes
 * {@code base_t:abs}. A model whose name is already plain keeps it.
 *
 * <p>
 * Names are chosen for the models one export writes together, and hold within
 * that export: the suffix a renamed model gets depends on which plain names
 * that export also holds.
 */
public final class PlainModelNames {

	private static final String SEPARATOR = ":";

	private final Map<String, String> renamed;

	private PlainModelNames(Map<String, String> renamed) {
		this.renamed = renamed;
	}

	/**
	 * The names of {@code modelNames}, which are written together in one
	 * export.
	 */
	public static PlainModelNames of(Collection<String> modelNames) {
		Set<String> taken = new HashSet<>();
		modelNames.stream().filter(n -> !n.contains(SEPARATOR))
				.forEach(taken::add);
		Map<String, String> renamed = new HashMap<>();
		modelNames.stream().filter(n -> n.contains(SEPARATOR)).sorted()
				.forEach(name -> {
					String base = name.replace(SEPARATOR, "_");
					String candidate = base;
					for (int n = 2; taken.contains(candidate); n++) {
						candidate = base + "_" + n;
					}
					taken.add(candidate);
					renamed.put(name, candidate);
				});
		return new PlainModelNames(renamed);
	}

	/** The name the export gives the model named {@code modelName}. */
	public String name(String modelName) {
		return renamed.getOrDefault(modelName, modelName);
	}

	/**
	 * {@code id}, an id of {@code owner}, with every model it names renamed.
	 * Which segments name a model is not read from the id, which is ambiguous
	 * once a model name holds a {@code :} ({@code b:ns:a:e}, #202): the id is
	 * rebuilt from where its element was copied from (see
	 * {@link JustificationModel#originOf}). A copy of an element {@code x} of a
	 * model {@code m} is {@code m:x}, or {@code x} itself when template
	 * expansion kept an id that was already qualified; an id with no origin is
	 * local, and unchanged.
	 */
	public String id(JustificationModel owner, String id) {
		if (renamed.isEmpty()) {
			return id;
		}
		return owner.originOf(id).map(origin -> {
			String inOrigin = id(origin.model(), origin.id());
			// The prefix the copy was given, which template expansion takes
			// from the implements rather than from the template's name.
			String prefix = id.substring(0,
					Math.max(0, id.length() - origin.id().length() - 1));
			return prefix.isEmpty()
					? inOrigin
					: name(prefix) + SEPARATOR + inOrigin;
		}).orElse(id);
	}

	/**
	 * {@code id}, an argument of an operator call on {@code sources} that names
	 * an element of the result ({@code source:id}), with every model it names
	 * renamed; see {@link QualifiedIdReadings#inResult}.
	 */
	public String resultArgument(List<JustificationModel> sources, String id) {
		return renamed.isEmpty()
				? id
				: rewrite(QualifiedIdReadings.inResult(sources, id,
						JustificationModel::getName));
	}

	/**
	 * {@code id}, an argument of an operator call on {@code sources} that names
	 * an element of a source as that source names it, with every model it names
	 * renamed; see {@link QualifiedIdReadings#inSources}. A short form is
	 * written in full, since the models it names may change name in the middle
	 * of the id.
	 */
	public String sourceArgument(List<JustificationModel> sources, String id) {
		if (renamed.isEmpty()) {
			return id;
		}
		List<Reading> readings = QualifiedIdReadings
				.inSources(sources, id, JustificationModel::getName).stream()
				.map(SourceReading::reading).toList();
		return readings.isEmpty() ? id : rewrite(readings);
	}

	/**
	 * The id the readings stand for, under the export's names. Two readings
	 * that stand for different ids mean the id names two elements: two elements
	 * of one model then share an id, which compilation reports
	 * ({@code no-duplicate-ids}). The export of such a model cannot be faithful
	 * anyway, so it keeps the first reading, in the order of the call's
	 * sources, rather than fail.
	 */
	private String rewrite(List<Reading> readings) {
		return QualifiedIdReadings.rewritten(readings, m -> name(m.getName()))
				.getFirst();
	}
}
