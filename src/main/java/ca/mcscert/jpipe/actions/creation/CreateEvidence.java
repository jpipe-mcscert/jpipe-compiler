package ca.mcscert.jpipe.actions.creation;

import ca.mcscert.jpipe.actions.Action;
import ca.mcscert.jpipe.actions.RegularAction;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Evidence;

/**
 * Implement the action used to create an evidence in a given justification.
 */
public final class CreateEvidence extends RegularAction {

    private final String container;
    private final String identifier;
    private final String label;

    /**
     * Provide necessary information to create an evidence in a justification.
     *
     * @param container the justification used as a container (its id).
     * @param identifier the evidence's justification.
     * @param label the evidence's justification.
     */
    public CreateEvidence(String container, String identifier, String label) {
        this.container = container;
        this.identifier = identifier;
        this.label = label;
    }

    @Override
    public void execute(Unit context) throws Exception {
        Evidence e = new Evidence(this.identifier, this.label);
        context.addInto(this.container, e);
    }

    @Override
    public String toString() {
        return "CreateEvidence{"
                + "container='" + container + '\''
                + ", identifier='" + identifier + '\''
                + ", label='" + label + '\''
                + '}';
    }

}
