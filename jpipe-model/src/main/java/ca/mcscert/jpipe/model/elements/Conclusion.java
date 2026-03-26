package ca.mcscert.jpipe.model.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The top-level conclusion that a justification aims to establish. */
public final class Conclusion implements CommonElement, Supportable<Strategy> {

	private final String id;
	private final String label;
	private final List<Strategy> supporters = new ArrayList<>();

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

	@Override
	public void addSupport(Strategy supporter) {
		supporters.add(supporter);
	}

	@Override
	public List<Strategy> getSupporters() {
		return Collections.unmodifiableList(supporters);
	}

	@Override
	public String toString() {
		return "Conclusion{id='" + id + "', label='" + label + "'}";
	}
}
