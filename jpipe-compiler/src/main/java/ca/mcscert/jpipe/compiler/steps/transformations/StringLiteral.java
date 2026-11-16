package ca.mcscert.jpipe.compiler.steps.transformations;

/**
 * The value of a {@code STRING} token: an element label, a {@code load} path,
 * or an operator parameter.
 *
 * <p>
 * The grammar only produces double-quoted tokens, in which {@code \"} stands
 * for a double quote and {@code \\} for a backslash. Any other backslash is
 * kept as written, so that a path such as {@code "dir\file.jd"} needs no
 * escaping.
 */
final class StringLiteral {

	private StringLiteral() {
	}

	/**
	 * Removes the quotes around {@code token} and resolves its escapes.
	 *
	 * @param token
	 *            the text of a {@code STRING} token, quotes included.
	 * @return the string it denotes.
	 * @throws IllegalArgumentException
	 *             if {@code token} is not a double-quoted literal: the grammar
	 *             produces no other, so this is a grammar/parser mismatch.
	 */
	static String value(String token) {
		if (token.length() < 2 || token.charAt(0) != '"'
				|| token.charAt(token.length() - 1) != '"') {
			throw new IllegalArgumentException(
					"Expected a double-quoted string literal, got: " + token);
		}
		int end = token.length() - 1;
		StringBuilder value = new StringBuilder(end);
		int i = 1;
		while (i < end) {
			char c = token.charAt(i);
			boolean escape = c == '\\' && i + 1 < end
					&& (token.charAt(i + 1) == '"'
							|| token.charAt(i + 1) == '\\');
			value.append(escape ? token.charAt(i + 1) : c);
			i += escape ? 2 : 1;
		}
		return value.toString();
	}
}
