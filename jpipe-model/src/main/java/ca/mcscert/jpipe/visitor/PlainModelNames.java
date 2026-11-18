package ca.mcscert.jpipe.visitor;

import java.util.ArrayList;
import java.util.Arrays;
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
	 * {@code id} with every run of segments that names a renamed model, longest
	 * first, replaced by that model's new name. Its last segment, the local id,
	 * is never a model name.
	 */
	public String id(String id) {
		if (renamed.isEmpty() || !id.contains(SEPARATOR)) {
			return id;
		}
		String[] segments = id.split(SEPARATOR);
		List<String> out = new ArrayList<>();
		int i = 0;
		while (i < segments.length - 1) {
			int length = longestModelAt(segments, i);
			if (length == 0) {
				out.add(segments[i]);
				i++;
			} else {
				out.add(renamed.get(join(segments, i, length)));
				i += length;
			}
		}
		out.add(segments[segments.length - 1]);
		return String.join(SEPARATOR, out);
	}

	/**
	 * The number of segments, from {@code start}, that name a renamed model,
	 * the longest such run, or {@code 0}. A renamed name has two segments at
	 * least, and the run leaves the last segment out.
	 */
	private int longestModelAt(String[] segments, int start) {
		for (int length = segments.length - 1 - start; length >= 2; length--) {
			if (renamed.containsKey(join(segments, start, length))) {
				return length;
			}
		}
		return 0;
	}

	private static String join(String[] segments, int start, int length) {
		return String.join(SEPARATOR,
				Arrays.copyOfRange(segments, start, start + length));
	}
}
