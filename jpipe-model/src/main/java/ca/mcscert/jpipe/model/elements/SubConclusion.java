package ca.mcscert.jpipe.model.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** An intermediate conclusion within a justification. */
public final class SubConclusion implements CommonElement, SupportLeaf, Supportable<Strategy> {

	private final String id;
	private final String label;
	private final List<Strategy> supporters = new ArrayList<>();

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
		supporters.add(supporter);
	}

	@Override
	public List<Strategy> getSupporters() {
		return Collections.unmodifiableList(supporters);
	}

	@Override
	public String toString() {
		return "SubConclusion{id='" + id + "', label='" + label + "'}";
	}
}
