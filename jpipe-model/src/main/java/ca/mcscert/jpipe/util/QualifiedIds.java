package ca.mcscert.jpipe.util;

/**
 * Helpers for colon-separated element ids ({@code templateName:s},
 * {@code ns:model:s}).
 */
public final class QualifiedIds {

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

	/**
	 * An element id as a file loaded under namespace {@code ns} names it in the
	 * unit: a qualified id names an element inherited from a model of the
	 * loaded file ({@code base:t:abs}), whose name the namespace prefixes too;
	 * a plain id is local and unchanged.
	 */
	public static String inNamespace(String ns, String id) {
		return id.contains(":") ? qualify(ns, id) : id;
	}

	/** Returns the last colon-separated segment of {@code id}. */
	public static String shortId(String id) {
		int colon = id.lastIndexOf(':');
		return colon >= 0 ? id.substring(colon + 1) : id;
	}
}
