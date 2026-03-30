package ca.mcscert.jpipe.commands.linking;

import ca.mcscert.jpipe.commands.RegularCommand;
import ca.mcscert.jpipe.model.JustificationModel;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SupportLeaf;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Replaces a strategy's current supporter with the element at
 * {@code newSupporterId}.
 */
public final class RewireStrategySupport extends RegularCommand {

	private final String container;
	private final String strategyId;
	private final String oldSupporterId;
	private final String newSupporterId;

	public RewireStrategySupport(String container, String strategyId,
			String newSupporterId) {
		this(container, strategyId, null, newSupporterId);
	}

	public RewireStrategySupport(String container, String strategyId,
			String oldSupporterId, String newSupporterId) {
		this.container = container;
		this.strategyId = strategyId;
		this.oldSupporterId = oldSupporterId;
		this.newSupporterId = newSupporterId;
	}

	@Override
	public Predicate<Unit> condition() {
		return unit -> unit.findModel(container).map(m -> {
			boolean base = m.findById(strategyId).isPresent()
					&& m.findById(newSupporterId).isPresent();
			if (oldSupporterId != null) {
				return base && m.findById(oldSupporterId).isPresent();
			}
			return base;
		}).orElse(false);
	}

	@Override
	protected void doExecute(Unit context) {
		JustificationModel<?> model = context.get(container);
		Strategy strategy = (Strategy) model.findById(strategyId)
				.orElseThrow(() -> new NoSuchElementException(
						"No strategy with id: " + strategyId));
		SupportLeaf newSupport = (SupportLeaf) model.findById(newSupporterId)
				.orElseThrow(() -> new NoSuchElementException(
						"No element with id: " + newSupporterId));

		if (oldSupporterId == null) {
			// Legacy behavior: if no old ID is provided, we can't replace
			// precisely.
			// But RewireStrategySupport is only used by
			// OverrideAbstractSupport now.
			// We'll just add it if old is not found, or replace if we find
			// something.
			// Actually, for multiple supports, we should always know what we
			// replace.
			strategy.addSupport(newSupport);
		} else {
			SupportLeaf oldSupport = (SupportLeaf) model
					.findById(oldSupporterId)
					.orElseThrow(() -> new NoSuchElementException(
							"No element with id: " + oldSupporterId));
			strategy.replaceSupport(oldSupport, newSupport);
		}
	}

	@Override
	public String toString() {
		return "rewire('" + container + "', '" + strategyId + "', '"
				+ oldSupporterId + "' -> '" + newSupporterId + "')";
	}
}
