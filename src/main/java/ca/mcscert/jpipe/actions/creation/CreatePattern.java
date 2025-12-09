package ca.mcscert.jpipe.actions.creation;

import ca.mcscert.jpipe.actions.RegularAction;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.Pattern;

/**
 * Create a Pattern inside a unit.
 */
public final class CreatePattern extends RegularAction {

    private final String identifier;
    private final String parent;

    /**
     * Provides information necessary to create a pattern.
     *
     * @param identifier the identified to be used to access it.
     */
    public CreatePattern(String identifier) {
        this.identifier = identifier;
        this.parent = null;
    }

    /**
     * Provides information necessary to create a pattern.
     *
     * @param identifier the identified to be used to access it.
     */
    public CreatePattern(String identifier, String parentId) {
        this.identifier = identifier;
        this.parent = parentId;
    }

    @Override
    public void execute(Unit context) throws Exception {
        context.add(new Pattern(this.identifier));
    }


    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CreatePattern{");
        sb.append(", identifier='").append(identifier).append('\'');
        sb.append(", parent='").append(parent).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
