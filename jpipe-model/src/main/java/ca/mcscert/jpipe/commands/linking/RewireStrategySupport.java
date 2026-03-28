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
	private final String newSupporterId;

	public RewireStrategySupport(String container, String strategyId,
			String newSupporterId) {
		this.container = container;
		this.strategyId = strategyId;
		this.newSupporterId = newSupporterId;
	}

	@Override
	public Predicate<Unit> condition() {
		return unit -> unit.findModel(container)
				.map(m -> m.findById(strategyId).isPresent()
						&& m.findById(newSupporterId).isPresent())
				.orElse(false);
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
		strategy.replaceSupport(newSupport);
	}

	@Override
	public String toString() {
		return "rewire('" + container + "', '" + strategyId + "', '"
				+ newSupporterId + "')";
	}
}
