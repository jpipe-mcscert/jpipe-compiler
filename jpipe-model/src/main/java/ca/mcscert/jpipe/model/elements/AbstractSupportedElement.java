package ca.mcscert.jpipe.model.elements;

import java.util.Optional;

/**
 * Package-private abstract base for {@link Conclusion} and
 * {@link SubConclusion}. Holds the shared {@code id}, {@code label}, and
 * {@code supporter} state and provides the common method implementations,
 * eliminating the structural duplication between the two classes.
 *
 * <p>
 * This class is an implementation detail — it is not part of any sealed
 * interface hierarchy and carries no domain semantics of its own. The sealed
 * contracts ({@link CommonElement}, {@link StrategyBacked}) are declared
 * directly on the concrete subclasses.
 */
abstract class AbstractSupportedElement {

	private final String id;
	private final String label;
	private Strategy supporter;

	protected AbstractSupportedElement(String id, String label) {
		this.id = id;
		this.label = label;
	}

	public String id() {
		return id;
	}

	public String label() {
		return label;
	}

	public void addSupport(Strategy supporter) {
		if (this.supporter != null) {
			throw new IllegalStateException(getClass().getSimpleName()
					+ " already has a supporting strategy");
		}
		this.supporter = supporter;
	}

	public Optional<Strategy> getSupport() {
		return Optional.ofNullable(supporter);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "{id='" + id + "', label='" + label
				+ "'}";
	}
}
