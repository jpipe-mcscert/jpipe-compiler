package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.commands.Command;
import ca.mcscert.jpipe.commands.MacroCommand;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.SourceLocation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;
import java.util.Optional;

/**
 * Replaces an {@link AbstractSupport} placeholder (inlined from a template)
 * with a concrete element. Expands into three atomic commands:
 * {@link RemoveElement}, {@link AddElement}, {@link RewireStrategySupport}.
 *
 * <p>
 * The condition defers execution until the abstract support is present in the
 * model (i.e., after {@link ImplementsTemplate} has run).
 */
public final class OverrideAbstractSupport implements MacroCommand {

	private final String container;
	private final String qualifiedId;
	private final String newType;
	private final String label;
	private final SourceLocation location;

	public OverrideAbstractSupport(String container, String qualifiedId,
			String newType, String label) {
		this(container, qualifiedId, newType, label, SourceLocation.UNKNOWN);
	}

	public OverrideAbstractSupport(String container, String qualifiedId,
			String newType, String label, SourceLocation location) {
		this.container = container;
		this.qualifiedId = qualifiedId;
		this.newType = newType;
		this.label = label;
		this.location = location;
	}

	public String container() {
		return container;
	}

	public String qualifiedId() {
		return qualifiedId;
	}

	public String newType() {
		return newType;
	}

	public String label() {
		return label;
	}

	@Override
	public SourceLocation location() {
		return location;
	}

	/**
	 * Ready once the placeholder is there, or once the id is ambiguous: then
	 * {@link #expand} reports it, whichever element comes first (#177).
	 */
	@Override
	public Predicate<Unit> condition() {
		return unit -> unit.findModel(container)
				.map(m -> !m.ambiguousMatches(qualifiedId).isEmpty() || m
						.findById(qualifiedId)
						.filter(AbstractSupport.class::isInstance).isPresent())
				.orElse(false);
	}

	/**
	 * The ids of the elements of {@code model} this override names: the one its
	 * id resolves to, or every one it is ambiguous between.
	 */
	public List<String> namedIds(JustificationModel model) {
		List<JustificationElement> ambiguous = model
				.ambiguousMatches(qualifiedId);
		return ambiguous.isEmpty()
				? model.findById(qualifiedId).map(JustificationElement::id)
						.stream().toList()
				: ambiguous.stream().map(JustificationElement::id).toList();
	}

	@Override
	public List<Command> expand(Unit unit) {
		JustificationModel model = unit.get(container);
		// A short id that names several inherited elements is ambiguous, as in
		// a relation: the element order must not choose the placeholder that
		// is replaced (#177).
		List<JustificationElement> ambiguous = model
				.ambiguousMatches(qualifiedId);
		if (!ambiguous.isEmpty()) {
			throw new AmbiguousIdException(qualifiedId, container, ambiguous);
		}
		AbstractSupport old = (AbstractSupport) model.findById(qualifiedId)
				.orElseThrow(() -> new NoSuchElementException(
						"No abstract support with id: " + qualifiedId));

		// Every strategy the placeholder supports is rewired, not only the
		// first: one left behind would keep the removed placeholder (#187). A
		// placeholder that supports none is still replaced: that is a mistake
		// of its template, reported there once, not in every justification
		// that overrides it (#183).
		List<String> strategyIds = model.strategiesSupportedBy(old.id())
				.stream().map(JustificationElement::id).toList();

		// Act on the id the placeholder actually has, which the short-name
		// fallback of findById may have resolved: removing and rewiring the
		// id as written would touch nothing, or something else (#178).
		String id = old.id();
		JustificationElement replacement = switch (newType) {
			case "evidence" -> new Evidence(id, label);
			case "sub-conclusion" -> new SubConclusion(id, label);
			default -> throw new IllegalArgumentException(
					"Cannot override abstract support with: " + newType);
		};

		unit.recordLocation(container, id, location);
		List<Command> commands = new ArrayList<>();
		commands.add(new RemoveElement(container, id));
		commands.add(new AddElement(container, replacement));
		strategyIds.forEach(
				strategyId -> commands.add(new RewireStrategySupport(container,
						strategyId, id, old.getClass().getSimpleName(), id,
						replacement.getClass().getSimpleName())));
		return commands;
	}

	@Override
	public Optional<String> target() {
		return Optional.of(container);
	}

	@Override
	public String toString() {
		return "override('" + container + "', '" + qualifiedId + "', " + newType
				+ ", '" + label + "')";
	}
}
