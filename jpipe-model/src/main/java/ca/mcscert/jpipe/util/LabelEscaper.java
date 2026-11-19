package ca.mcscert.jpipe.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Converts a human-readable jPipe element label into a valid Python identifier
 * (snake_case).
 *
 * <p>
 * The conversion runs in three phases:
 * <ol>
 * <li>Unicode NFKC normalisation, the form Python itself gives identifiers,
 * then folding of every character that reduces to ASCII once its diacritics are
 * stripped (e.g. {@code é} → {@code e}, {@code ü} → {@code u}). Any other
 * letter is kept as written: Python 3 accepts Unicode identifiers, so a label
 * in a non-Latin script keeps its words.</li>
 * <li>Lowercasing, independently of the default locale, collapsing any run of
 * characters that are not letters, digits or combining marks to a single
 * underscore, and stripping leading/trailing underscores.</li>
 * <li>An underscore prefix when the result is empty or cannot start an
 * identifier, such as a leading digit.</li>
 * </ol>
 *
 * <p>
 * Examples:
 * <ul>
 * <li>{@code "The system is correct"} → {@code "the_system_is_correct"}</li>
 * <li>{@code "Accuracy <= 0.85"} → {@code "accuracy_0_85"}</li>
 * <li>{@code "café test"} → {@code "cafe_test"}</li>
 * <li>{@code "100% accurate"} → {@code "_100_accurate"}</li>
 * <li>{@code "系统 正确"} → {@code "系统_正确"}</li>
 * </ul>
 */
public final class LabelEscaper {

	/**
	 * A run of characters a Python identifier cannot hold: anything but a
	 * letter, a decimal digit or a combining mark. U+2E2F VERTICAL TILDE is the
	 * one letter outside them: it has the {@code Pattern_Syntax} property,
	 * which UAX #31 excludes from identifiers. That property is immutable, so
	 * no later Unicode version adds another.
	 */
	private static final Pattern NOT_IN_IDENTIFIER = Pattern
			.compile("[[^\\p{L}\\p{Nd}\\p{Mn}\\p{Mc}]\\x{2E2F}]+");

	private static final Pattern EDGE_UNDERSCORES = Pattern
			.compile("(^_+)|(_+$)");

	private static final Pattern MARKS = Pattern.compile("\\p{M}");

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
		String folded = Normalizer.normalize(label, Normalizer.Form.NFKC)
				.codePoints().mapToObj(LabelEscaper::foldToAscii)
				.collect(Collectors.joining());
		String snake = EDGE_UNDERSCORES.matcher(NOT_IN_IDENTIFIER
				.matcher(folded.toLowerCase(Locale.ROOT)).replaceAll("_"))
				.replaceAll("");
		String name = snake.isEmpty()
				|| !Character.isUnicodeIdentifierStart(snake.codePointAt(0))
						? "_" + snake
						: snake;
		return Normalizer.normalize(name, Normalizer.Form.NFKC);
	}

	/**
	 * The ASCII base of {@code codePoint} when stripping its diacritics leaves
	 * one, the code point itself otherwise: {@code é} becomes {@code e}, but a
	 * CJK ideograph or a Devanagari vowel sign is kept.
	 */
	private static String foldToAscii(int codePoint) {
		String stripped = MARKS.matcher(Normalizer
				.normalize(Character.toString(codePoint), Normalizer.Form.NFD))
				.replaceAll("");
		return !stripped.isEmpty() && stripped.chars().allMatch(c -> c < 0x80)
				? stripped
				: Character.toString(codePoint);
	}
}
