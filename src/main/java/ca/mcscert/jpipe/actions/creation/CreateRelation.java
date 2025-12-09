package ca.mcscert.jpipe.actions.creation;

import ca.mcscert.jpipe.actions.RegularAction;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.JustificationModel;

/**
 * Action to be used to create a relation between two elements inside a justification.
 */
public final class CreateRelation extends RegularAction {

    private final String container;
    private final String from;
    private final String to;

    /**
     * Information necessary to create a relation between elements inside a justification.
     * Here, the relation is read as in "from supports to".
     *
     * @param container the justification containing the elements.
     * @param from the element on the left-hand side of the relation.
     * @param to the element on the right-hand side of the justification.
     */
    public CreateRelation(String container, String from, String to) {
        this.container = container;
        this.from = from;
        this.to = to;
    }

    @Override
    public void execute(Unit context) throws Exception {
        JustificationModel justification = context.get(container);
        JustificationElement f = justification.get(from);
        JustificationElement t = justification.get(to);
        f.supports(t);
    }

    @Override
    public String toString() {
        return "CreateRelation{"
                + "container='" + container + '\''
                + ", from='" + from + '\''
                + ", to='" + to + '\''
                + '}';
    }
}
