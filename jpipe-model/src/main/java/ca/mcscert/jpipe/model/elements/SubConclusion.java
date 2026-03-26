package ca.mcscert.jpipe.model.elements;

import java.util.Optional;

/** An intermediate conclusion within a justification. */
public final class SubConclusion implements CommonElement, SupportLeaf, Supportable<Strategy> {

	private final String id;
	private final String label;
	private Strategy supporter;

	public SubConclusion(String id, String label) {
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

	@Override
	public void addSupport(Strategy supporter) {
		if (this.supporter != null) {
			throw new IllegalStateException("SubConclusion already has a supporting strategy");
		}
		this.supporter = supporter;
	}

	@Override
	public Optional<Strategy> getSupport() {
		return Optional.ofNullable(supporter);
	}

	@Override
	public String toString() {
		return "SubConclusion{id='" + id + "', label='" + label + "'}";
	}
}
