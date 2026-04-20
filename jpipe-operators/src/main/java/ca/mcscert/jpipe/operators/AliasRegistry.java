package ca.mcscert.jpipe.operators;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable registry that maps original element ids to the merged id chosen
 * during Phase 1 of {@link CompositionOperator#apply}.
 *
 * <p>
 * Merge functions populate this registry by calling
 * {@link #register(String, List)} or {@link #register(String, String)}.
 * {@link CompositionOperator} then uses it in Phase 2 to translate original
 * edge endpoints to their merged counterparts, and emits one
 * {@link ca.mcscert.jpipe.commands.linking.RegisterAlias} command per entry so
 * the mapping survives in the compiled {@link ca.mcscert.jpipe.model.Unit}.
 */
public final class AliasRegistry {

	private final Map<String, String> aliases = new LinkedHashMap<>();

	/**
	 * Records that {@code newId} is the merged identity of all {@code oldIds}.
	 * Typically called by a merge function once it has chosen the new element
	 * id.
	 */
	public void register(String newId, List<String> oldIds) {
		oldIds.forEach(old -> aliases.put(old, newId));
	}

	/** Convenience single-id overload. */
	public void register(String newId, String oldId) {
		aliases.put(oldId, newId);
	}

	/**
	 * Returns the new id that {@code oldId} was merged into, or {@code oldId}
	 * itself if no alias was registered (element was kept as-is).
	 */
	public String resolve(String oldId) {
		return aliases.getOrDefault(oldId, oldId);
	}

	/** Read-only view of all registered aliases. */
	public Map<String, String> aliases() {
		return Collections.unmodifiableMap(aliases);
	}
}
