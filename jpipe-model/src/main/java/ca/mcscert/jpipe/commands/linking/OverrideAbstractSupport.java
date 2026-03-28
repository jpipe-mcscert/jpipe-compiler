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
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

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

	@Override
	public Predicate<Unit> condition() {
		return unit -> unit.findModel(container)
				.map(m -> m.findById(qualifiedId)
						.filter(AbstractSupport.class::isInstance).isPresent())
				.orElse(false);
	}

	@Override
	public List<Command> expand(Unit unit) {
		JustificationModel<?> model = unit.get(container);
		AbstractSupport old = (AbstractSupport) model.findById(qualifiedId)
				.orElseThrow(() -> new NoSuchElementException(
						"No abstract support with id: " + qualifiedId));

		String strategyId = model.strategies().stream().filter(s -> s
				.getSupport()
				.filter(sl -> ((JustificationElement) sl).id().equals(old.id()))
				.isPresent()).map(JustificationElement::id).findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"No strategy supports abstract support: "
								+ qualifiedId));

		JustificationElement replacement = switch (newType) {
			case "evidence" -> new Evidence(qualifiedId, label);
			case "sub-conclusion" -> new SubConclusion(qualifiedId, label);
			default -> throw new IllegalArgumentException(
					"Cannot override abstract support with: " + newType);
		};

		unit.recordLocation(container, qualifiedId, location);
		return List.of(new RemoveElement(container, qualifiedId),
				new AddElement(container, replacement),
				new RewireStrategySupport(container, strategyId, qualifiedId));
	}

	@Override
	public String toString() {
		return "override('" + container + "', '" + qualifiedId + "', " + newType
				+ ", '" + label + "')";
	}
}
