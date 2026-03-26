package ca.mcscert.jpipe.model.elements;

import java.util.Optional;

/** The top-level conclusion that a justification aims to establish. */
public final class Conclusion implements CommonElement {

	private final String id;
	private final String label;
	private Strategy supporter;

	public Conclusion(String id, String label) {
		this.id = id;
		this.label = label;
	}

	@Override
	public String id() {
		return id;
	}

	@Override
	public String label() {
		return label;
	}

	public void addSupport(Strategy supporter) {
		if (this.supporter != null) {
			throw new IllegalStateException("Conclusion already has a supporting strategy");
		}
		this.supporter = supporter;
	}

	public Optional<Strategy> getSupport() {
		return Optional.ofNullable(supporter);
	}

	@Override
	public String toString() {
		return "Conclusion{id='" + id + "', label='" + label + "'}";
	}
}
