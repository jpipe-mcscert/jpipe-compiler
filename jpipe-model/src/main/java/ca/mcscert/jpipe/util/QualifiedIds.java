package ca.mcscert.jpipe.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Helpers for colon-separated element ids ({@code templateName:s},
 * {@code ns:model:s}).
 */
public final class QualifiedIds {

	private static final String SEPARATOR = ":";

	private QualifiedIds() {
	}

	/** Returns {@code id} qualified with {@code prefix}: {@code prefix:id}. */
	public static String qualify(String prefix, String id) {
		return prefix + ":" + id;
	}

	/**
	 * Returns {@code id} qualified with {@code prefix} when it is a plain id
	 * (no colon), or {@code id} unchanged when it is already qualified. This is
	 * the rule template expansion applies: an id inherited from a grandparent
	 * template already carries its qualifier and must not be qualified twice.
	 */
	public static String qualifyIfPlain(String prefix, String id) {
		return id.contains(":") ? id : qualify(prefix, id);
	}

	/** Returns the last colon-separated segment of {@code id}. */
	public static String shortId(String id) {
		int colon = id.lastIndexOf(':');
		return colon >= 0 ? id.substring(colon + 1) : id;
	}

	/**
	 * Returns {@code id} with each run of its segments that names a model
	 * replaced by the name {@code models} gives it, the longest run first.
	 * {@code models} maps a run, such as {@code base:t}, to its new name, or to
	 * nothing when the run names no model. The last segment, the local id, is
	 * never part of a run, so a plain id is returned unchanged.
	 *
	 * <p>
	 * Which segments name a model, and which, is known only from the models
	 * that exist: {@code both:a:e} holds the model {@code a} only when
	 * {@code both} is not itself named {@code both:a}.
	 */
	public static String renameModels(String id,
			Function<String, Optional<String>> models) {
		String[] segments = id.split(SEPARATOR, -1);
		int last = segments.length - 1;
		List<String> out = new ArrayList<>();
		int i = 0;
		while (i < last) {
			int length = last - i;
			Optional<String> renamed = models.apply(join(segments, i, length));
			while (renamed.isEmpty() && length > 1) {
				length--;
				renamed = models.apply(join(segments, i, length));
			}
			out.add(renamed.orElse(segments[i]));
			i += renamed.isPresent() ? length : 1;
		}
		out.add(segments[last]);
		return String.join(SEPARATOR, out);
	}

	private static String join(String[] segments, int start, int length) {
		return String.join(SEPARATOR,
				Arrays.copyOfRange(segments, start, start + length));
	}
}
