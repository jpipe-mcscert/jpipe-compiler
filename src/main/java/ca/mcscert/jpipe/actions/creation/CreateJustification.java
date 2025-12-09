package ca.mcscert.jpipe.actions.creation;

import ca.mcscert.jpipe.actions.RegularAction;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Justification;

/**
 * Create a justification inside a unit.
 */
public final class CreateJustification extends RegularAction {

    private final String identifier;
    private final String parent;

    /**
     * Provides information necessary to create a justification.
     *
     * @param identifier the identified to be used to access it.
     */
    public CreateJustification(String identifier) {
        this.identifier = identifier;
        this.parent = null;
    }

    /**
     * Provides information necessary to create a justification.
     *
     * @param identifier the identified to be used to access it.
     */
    public CreateJustification(String identifier, String parentId) {
        this.identifier = identifier;
        this.parent = parentId;
    }


    @Override
    public void execute(Unit context) throws Exception {
        context.add(new Justification(this.identifier));
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CreateJustification{");
        sb.append("identifier='").append(identifier).append('\'');
        sb.append(", parent='").append(parent).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
