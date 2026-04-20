package ca.mcscert.jpipe.operators;

/**
 * Name-to-{@link CompositionOperator} registry. Populated at compiler startup
 * with built-in operators; looked up at model-build time by
 * {@link ApplyOperator}.
 */
public final class OperatorRegistry extends Registry<CompositionOperator> {

	/** Registers {@code operator} under {@code name}. */
	public void register(String name, CompositionOperator operator) {
		put(name, operator);
	}
}
