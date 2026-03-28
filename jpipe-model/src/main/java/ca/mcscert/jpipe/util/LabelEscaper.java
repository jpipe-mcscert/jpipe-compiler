package ca.mcscert.jpipe.util;

import java.text.Normalizer;

/**
 * Converts a human-readable jPipe element label into a valid Python identifier
 * (snake_case).
 *
 * <p>
 * The conversion is two-phase:
 * <ol>
 * <li>Unicode NFD normalisation followed by stripping of combining diacritics,
 * so accented characters map to their ASCII base (e.g. {@code é} → {@code e},
 * {@code ü} → {@code u}).</li>
 * <li>Lowercasing, collapsing any run of non-alphanumeric characters to a
 * single underscore, and stripping leading/trailing underscores. If the result
 * starts with a digit an underscore prefix is added.</li>
 * </ol>
 *
 * <p>
 * Examples:
 * <ul>
 * <li>{@code "The system is correct"} → {@code "the_system_is_correct"}</li>
 * <li>{@code "Accuracy <= 0.85"} → {@code "accuracy_0_85"}</li>
 * <li>{@code "café test"} → {@code "cafe_test"}</li>
 * <li>{@code "100% accurate"} → {@code "_100_accurate"}</li>
 * </ul>
 */
public final class LabelEscaper {

	private LabelEscaper() {
	}

	/**
	 * Convert {@code label} to a valid snake_case Python identifier.
	 *
	 * @param label
	 *            the element label to convert; must not be {@code null}.
	 * @return a non-empty string that is a valid Python identifier.
	 */
	public static String toMethodName(String label) {
		String ascii = Normalizer.normalize(label, Normalizer.Form.NFD)
				.replaceAll("\\p{M}", "");
		String snake = ascii.toLowerCase().replaceAll("[^a-z0-9]+", "_")
				.replaceAll("^_+|_+$", "");
		return snake.isEmpty() || Character.isDigit(snake.charAt(0))
				? "_" + snake
				: snake;
	}
}
