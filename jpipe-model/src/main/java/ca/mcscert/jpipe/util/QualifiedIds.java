package ca.mcscert.jpipe.util;

/**
 * Helpers for colon-separated element ids ({@code templateName:s},
 * {@code ns:model:s}).
 */
public final class QualifiedIds {

	private QualifiedIds() {
	}

	/**
	 * Returns {@code id} qualified with {@code prefix} when it is a plain id
	 * (no colon), or {@code id} unchanged when it is already qualified. This is
	 * the rule template expansion applies: an id inherited from a grandparent
	 * template already carries its qualifier and must not be qualified twice.
	 */
	public static String qualifyIfPlain(String id, String prefix) {
		return id.contains(":") ? id : prefix + ":" + id;
	}

	/** Returns the last colon-separated segment of {@code id}. */
	public static String shortId(String id) {
		int colon = id.lastIndexOf(':');
		return colon >= 0 ? id.substring(colon + 1) : id;
	}
}
