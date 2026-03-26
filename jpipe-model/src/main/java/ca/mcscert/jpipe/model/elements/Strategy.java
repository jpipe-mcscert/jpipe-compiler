package ca.mcscert.jpipe.model.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A reasoning strategy connecting evidence to a conclusion. */
public final class Strategy implements CommonElement, Supportable<SupportLeaf> {

	private final String id;
	private final String label;
	private final List<SupportLeaf> supporters = new ArrayList<>();

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

	@Override
	public void addSupport(SupportLeaf supporter) {
		supporters.add(supporter);
	}

	@Override
	public List<SupportLeaf> getSupporters() {
		return Collections.unmodifiableList(supporters);
	}

	@Override
	public String toString() {
		return "Strategy{id='" + id + "', label='" + label + "'}";
	}
}
