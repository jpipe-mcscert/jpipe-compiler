package ca.mcscert.jpipe.util;

import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

/**
 * Alias chains: composition records each merged id as an alias of the id it was
 * merged into, one link per composition step.
 */
public final class Aliases {

	private Aliases() {
	}

	/**
	 * {@code id} followed through {@code next}, which gives the id an id was
	 * merged into, or {@code null}, to the end of its chain: {@code id} itself
	 * when it is no alias. A chain that loops stops where it would repeat.
	 */
	public static String follow(String id, UnaryOperator<String> next) {
		String current = id;
		Set<String> visited = new HashSet<>();
		String following = next.apply(current);
		while (following != null && visited.add(current)) {
			current = following;
			following = next.apply(current);
		}
		return current;
	}
}
