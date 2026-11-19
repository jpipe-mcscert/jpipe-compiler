package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.QualifiedIdReadings;
import ca.mcscert.jpipe.model.QualifiedIdReadings.Reading;
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
	 * Which segments name a model is read by provenance, through the models
	 * {@code owner} is built from (see {@link QualifiedIdReadings}): in
	 * {@code b:ns:a:e}, {@code b:ns} names a model only when the rest,
	 * {@code a:e}, is an id of that model (#202).
	 */
	public String id(JustificationModel owner, String id) {
		return renamed.isEmpty()
				? id
				: rewrite(QualifiedIdReadings.in(owner, id,
						JustificationModel::getName));
	}

	/**
	 * {@code id}, an id given as an argument of an operator call on
	 * {@code sources}, with every model it names renamed; see
	 * {@link QualifiedIdReadings#inCall}.
	 */
	public String argument(List<JustificationModel> sources, String id) {
		return renamed.isEmpty()
				? id
				: rewrite(QualifiedIdReadings.inCall(sources, id,
						JustificationModel::getName));
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
