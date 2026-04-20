package ca.mcscert.jpipe.visitor;

import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Template;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-static helpers for producing DOT-safe label strings.
 *
 * <p>
 * All methods are stateless and produce strings; they carry no dependency on
 * the exporter state.
 */
final class DotLabel {

	private static final int WRAP_WIDTH = 40;

	private DotLabel() {
	}

	/**
	 * Returns {@code value} wrapped in DOT double-quotes, with internal
	 * backslashes and double-quotes escaped.
	 */
	static String quoted(String value) {
		return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	/**
	 * Wraps {@code label} at word boundaries so no line exceeds
	 * {@link #WRAP_WIDTH} characters, then returns the result as a DOT-quoted
	 * string. Lines are separated by {@code \n}, which Graphviz renders as a
	 * centred newline inside a node label.
	 *
	 * <p>
	 * Each word is escaped for backslashes, double-quotes, and underscores
	 * before measuring and joining, so wrap decisions are based on the
	 * characters that will actually appear in the rendered label.
	 */
	static String wrapAndQuote(String label) {
		String[] words = label.trim().split("\\s+");
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : words) {
			String escaped = word.replace("\\", "\\\\").replace("\"", "\\\"")
					.replace("_", "\\_");
			if (!current.isEmpty()
					&& current.length() + 1 + escaped.length() > WRAP_WIDTH) {
				lines.add(current.toString());
				current = new StringBuilder(escaped);
			} else {
				if (!current.isEmpty())
					current.append(' ');
				current.append(escaped);
			}
		}
		if (!current.isEmpty())
			lines.add(current.toString());
		return "\"" + String.join("\\n", lines) + "\"";
	}

	/**
	 * Returns the display label for {@code model}: template models are prefixed
	 * with {@code <<template>>} to distinguish them from concrete
	 * justifications.
	 */
	static String display(JustificationModel<?> model) {
		return model instanceof Template
				? "<<template>> " + model.getName()
				: model.getName();
	}
}
