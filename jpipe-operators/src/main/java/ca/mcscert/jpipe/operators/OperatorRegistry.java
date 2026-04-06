package ca.mcscert.jpipe.operators;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Name-to-{@link CompositionOperator} registry. Populated at compiler startup
 * with built-in operators; looked up at model-build time by
 * {@link ApplyOperator}.
 */
public final class OperatorRegistry {

	private final Map<String, CompositionOperator> operators = new LinkedHashMap<>();

	/** Registers {@code operator} under {@code name}. */
	public void register(String name, CompositionOperator operator) {
		operators.put(name, operator);
	}

	/**
	 * Returns the operator registered under {@code name}, or empty if none was
	 * registered.
	 */
	public Optional<CompositionOperator> find(String name) {
		return Optional.ofNullable(operators.get(name));
	}

	/** Read-only view of all registered operator names. */
	public Set<String> registeredNames() {
		return Collections.unmodifiableSet(operators.keySet());
	}
}
