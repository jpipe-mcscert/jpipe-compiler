package ca.mcscert.jpipe.model.elements;

import java.util.Optional;

/** A reasoning strategy connecting evidence to a conclusion. */
public final class Strategy implements CommonElement {

	private final String id;
	private final String label;
	private SupportLeaf supporter;

	public Strategy(String id, String label) {
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

	public void addSupport(SupportLeaf supporter) {
		if (this.supporter != null) {
			throw new IllegalStateException("Strategy already has a supporting element");
		}
		this.supporter = supporter;
	}

	public Optional<SupportLeaf> getSupport() {
		return Optional.ofNullable(supporter);
	}

	@Override
	public String toString() {
		return "Strategy{id='" + id + "', label='" + label + "'}";
	}
}
