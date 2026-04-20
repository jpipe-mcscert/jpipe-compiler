package ca.mcscert.jpipe.operators;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Package-private abstract base for named-strategy registries.
 *
 * <p>
 * Provides a {@link LinkedHashMap}-backed store with {@link #find} and
 * {@link #registeredNames} accessible to all callers, and a
 * {@link #put(String, Object) put} mutator restricted to subclasses. Concrete
 * registries ({@link OperatorRegistry}, {@link UnificationEquivalenceRegistry})
 * expose domain-specific {@code register} methods that delegate to
 * {@link #put}.
 *
 * <p>
 * {@link AliasRegistry} is intentionally excluded: it is a transient per-call
 * accumulator with inverted key/value semantics and a different lifecycle.
 *
 * @param <V>
 *            the type of value stored in this registry
 */
abstract class Registry<V> {

	private final Map<String, V> entries = new LinkedHashMap<>();

	/** Stores {@code value} under {@code name}. */
	protected final void put(String name, V value) {
		entries.put(name, value);
	}

	/**
	 * Returns the value registered under {@code name}, or empty if none was
	 * registered.
	 */
	public Optional<V> find(String name) {
		return Optional.ofNullable(entries.get(name));
	}

	/** Read-only view of all registered names. */
	public Set<String> registeredNames() {
		return Collections.unmodifiableSet(entries.keySet());
	}
}
