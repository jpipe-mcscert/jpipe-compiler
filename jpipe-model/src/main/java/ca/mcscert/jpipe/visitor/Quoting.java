package ca.mcscert.jpipe.visitor;

/**
 * The one escaping rule the exporters share: a backslash and a double quote are
 * each preceded by a backslash. jPipe strings, DOT strings and Python string
 * literals all read it the same way, so each exporter writes text through here
 * rather than through a copy of the rule.
 */
final class Quoting {

	private Quoting() {
	}

	/** {@code value} with its backslashes and double quotes escaped. */
	static String escaped(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	/** {@code value} escaped, between double quotes. */
	static String doubleQuoted(String value) {
		return "\"" + escaped(value) + "\"";
	}
}
