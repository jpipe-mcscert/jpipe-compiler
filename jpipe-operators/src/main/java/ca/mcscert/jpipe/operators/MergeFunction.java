package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.commands.Command;
import java.util.List;

/**
 * Describes how to create the merged element for one {@link ElementGroup}.
 *
 * <p>
 * Contract:
 * <ul>
 * <li>MUST call {@code aliases.register(newId, oldIds)} with the chosen new
 * element id and the ids of all group members.</li>
 * <li>MUST NOT emit {@code AddSupport} commands — support edge reconstruction
 * is handled automatically by {@link CompositionOperator} in Phase 2 using the
 * completed {@link AliasRegistry}.</li>
 * </ul>
 */
@FunctionalInterface
public interface MergeFunction {

	/**
	 * Creates the merged element for {@code group} inside
	 * {@code resultModelName} and registers the mapping in {@code aliases}.
	 *
	 * @param resultModelName
	 *            name of the result model being built
	 * @param group
	 *            the equivalence class to merge
	 * @param aliases
	 *            registry to record old-id → new-id mappings
	 * @return commands that create the merged element (no {@code AddSupport})
	 */
	List<Command> merge(String resultModelName, ElementGroup group,
			AliasRegistry aliases);
}
