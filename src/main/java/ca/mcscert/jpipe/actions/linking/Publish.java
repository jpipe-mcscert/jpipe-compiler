package ca.mcscert.jpipe.actions.linking;

import ca.mcscert.jpipe.actions.RegularAction;
import ca.mcscert.jpipe.error.UnknownSymbol;
import ca.mcscert.jpipe.model.Unit;
import java.util.function.Function;

/**
 * Publish a justification model, i.e., indicates it is ready to be used by others.
 */
public class Publish extends RegularAction {

    private final String identifier;

    public Publish(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public Function<Unit, Boolean> condition() {
        return (u -> {
            try {
                u.get(identifier);
            } catch (UnknownSymbol s) {
                return false;
            }
            return true;
        });
    }

    @Override
    public void execute(Unit context) throws Exception {
        context.get(identifier).freeze();
    }


    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Publish{");
        sb.append("identifier='").append(identifier).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
