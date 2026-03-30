package ca.mcscert.jpipe.model.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A reasoning strategy connecting evidence to a conclusion. */
public final class Strategy implements CommonElement {

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

	public void addSupport(SupportLeaf supporter) {
		this.supporters.add(supporter);
	}

	public List<SupportLeaf> getSupports() {
		return Collections.unmodifiableList(supporters);
	}

	/**
	 * Replaces {@code oldSupport} with {@code newSupport} in the list of
	 * supporters.
	 */
	public void replaceSupport(SupportLeaf oldSupport, SupportLeaf newSupport) {
		int index = supporters.indexOf(oldSupport);
		if (index != -1) {
			supporters.set(index, newSupport);
		}
	}

	@Override
	public String toString() {
		return "Strategy{id='" + id + "', label='" + label + "'}";
	}
}
