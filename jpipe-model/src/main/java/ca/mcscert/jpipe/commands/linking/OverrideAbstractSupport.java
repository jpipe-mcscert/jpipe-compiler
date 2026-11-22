package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.util.QualifiedIds;
import ca.mcscert.jpipe.model.Binding;
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
				.map(m -> switch (m.bind(qualifiedId)) {
					case Binding.Found(JustificationElement e) ->
						e instanceof AbstractSupport;
					case Binding.Ambiguous _ -> true;
					case Binding.Missing() -> false;
				}).orElse(false);
	}

	/**
	 * The ids of the elements of {@code model} this override names: the one its
	 * id resolves to, or every one it is ambiguous between.
	 */
	public List<String> namedIds(JustificationModel model) {
		return switch (model.bind(qualifiedId)) {
			case Binding.Found(JustificationElement e) -> List.of(e.id());
			case Binding.Ambiguous(List<JustificationElement> candidates) ->
				candidates.stream().map(JustificationElement::id).toList();
			case Binding.Missing() -> List.of();
		};
	}

	@Override
	public List<Command> expand(Unit unit) {
		JustificationModel model = unit.get(container);
		// A short id that names several inherited elements is ambiguous, as in
		// a relation: the element order must not choose the placeholder that
		// is replaced (#177).
		AbstractSupport old = switch (model.bind(qualifiedId)) {
			case Binding.Found(AbstractSupport support) -> support;
			case Binding.Ambiguous(List<JustificationElement> candidates) ->
				throw new AmbiguousIdException(qualifiedId, container,
						candidates);
			case Binding.Found _,Binding.Missing _ ->
				throw new NoSuchElementException(
						"No abstract support with id: " + qualifiedId);
		};

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

	@Override
	public OverrideAbstractSupport withNamespace(String ns) {
		return new OverrideAbstractSupport(QualifiedIds.qualify(ns, container),
				QualifiedIds.inNamespace(ns, qualifiedId), newType, label,
				location);
	}
}
