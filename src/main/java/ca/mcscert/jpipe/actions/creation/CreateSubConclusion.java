package ca.mcscert.jpipe.actions.creation;

import ca.mcscert.jpipe.actions.Action;
import ca.mcscert.jpipe.actions.RegularAction;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.SubConclusion;

/**
 * Action used to create a sub-conclusion inside a justification.
 */
public final class CreateSubConclusion extends RegularAction {

    private final String container;
    private final String identifier;
    private final String label;

    /**
     * Information necessary to create a SubConclusion inside a given Justification.
     *
     * @param container the justification that will contain the sub-conclusion (it's identifier).
     * @param identifier the sub-conclusion's identifier.
     * @param label the sub-conclusion's label.
     */
    public CreateSubConclusion(String container, String identifier, String label) {
        this.container = container;
        this.identifier = identifier;
        this.label = label;
    }

    @Override
    public void execute(Unit context) throws Exception {
        SubConclusion sc = new SubConclusion(identifier, label);
        context.addInto(container, sc);

    }

    @Override
    public String toString() {
        return "CreateSubConclusion{"
                + "container='" + container + '\''
                + ", identifier='" + identifier + '\''
                + ", label='" + label + '\''
                + '}';
    }
}
