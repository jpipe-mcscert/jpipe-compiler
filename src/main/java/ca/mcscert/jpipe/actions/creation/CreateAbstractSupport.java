package ca.mcscert.jpipe.actions.creation;

import ca.mcscert.jpipe.actions.Action;
import ca.mcscert.jpipe.actions.RegularAction;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;

/**
 * Implement the action used to create an evidence in a given justification.
 */
public final class CreateAbstractSupport extends RegularAction {

    private final String container;
    private final String identifier;
    private final String label;

    /**
     * Provide necessary information to create an abstract support in a justification.
     *
     * @param container the justification used as a container (its id).
     * @param identifier the evidence's justification.
     * @param label the evidence's justification.
     */
    public CreateAbstractSupport(String container, String identifier, String label) {
        this.container = container;
        this.identifier = identifier;
        this.label = label;
    }

    @Override
    public void execute(Unit context) throws Exception {
        AbstractSupport s = new AbstractSupport(this.identifier, this.label);
        context.addInto(this.container, s);
    }

    @Override
    public String toString() {
        return "CreateAbstractSupport{"
                + "container='" + container + '\''
                + ", identifier='" + identifier + '\''
                + ", label='" + label + '\''
                + '}';
    }

}
