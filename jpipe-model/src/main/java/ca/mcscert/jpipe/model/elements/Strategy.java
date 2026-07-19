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

	/**
	 * Adds {@code supporter} to this strategy. A strategy can never
	 * legitimately be supported twice by the same element id, so the call is
	 * idempotent by id: if a supporter with the same id is already present,
	 * nothing is added.
	 */
	public void addSupport(SupportLeaf supporter) {
		String supporterId = ((JustificationElement) supporter).id();
		boolean present = supporters.stream().anyMatch(
				s -> ((JustificationElement) s).id().equals(supporterId));
		if (!present) {
			this.supporters.add(supporter);
		}
	}

	public List<SupportLeaf> getSupports() {
		return Collections.unmodifiableList(supporters);
	}

	/**
	 * Replaces the supporter sharing {@code oldSupport}'s id with
	 * {@code newSupport}. Matching is by id rather than object identity so that
	 * an {@link AbstractSupport} placeholder can be swapped for the concrete
	 * element that overrides it (both share the same qualified id but are
	 * different objects and types).
	 */
	public void replaceSupport(SupportLeaf oldSupport, SupportLeaf newSupport) {
		String oldId = ((JustificationElement) oldSupport).id();
		for (int i = 0; i < supporters.size(); i++) {
			if (((JustificationElement) supporters.get(i)).id().equals(oldId)) {
				supporters.set(i, newSupport);
				return;
			}
		}
	}

	@Override
	public String toString() {
		return "Strategy{id='" + id + "', label='" + label + "'}";
	}
}
