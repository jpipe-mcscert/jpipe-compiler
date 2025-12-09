package ca.mcscert.jpipe.actions.creation;

import ca.mcscert.jpipe.actions.Action;
import ca.mcscert.jpipe.actions.RegularAction;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Strategy;

/**
 * Action tp create a strategy inside a justification.
 */
public final class CreateStrategy extends RegularAction {

    private final String container;
    private final String identifier;
    private final String label;

    /**
     * Provides necessary information to create a Strategy inside a justification.
     *
     * @param container the justification containing the strategy.
     * @param identifier the strategy's identifier.
     * @param label the strategy's label.
     */
    public CreateStrategy(String container, String identifier, String label) {
        this.container = container;
        this.identifier = identifier;
        this.label = label;
    }

    @Override
    public void execute(Unit context) throws Exception {
        context.addInto(container, new Strategy(identifier, label));
    }

    @Override
    public String toString() {
        return "CreateStrategy{"
                + "container='" + container + '\''
                + ", identifier='" + identifier + '\''
                + ", label='" + label + '\''
                + '}';
    }

}
