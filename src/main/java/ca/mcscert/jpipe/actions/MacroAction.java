package ca.mcscert.jpipe.actions;

import ca.mcscert.jpipe.model.Unit;
import java.util.List;

/**
 * A macro action is an action that is not executable per se. but, instead refined into a list
 * of action to be executed.
 */
public abstract class MacroAction implements Action {

    @Override
    public final boolean isExpandable() {
        return true;
    }

    @Override
    public final void execute(Unit context) throws Exception {
        throw new UnsupportedOperationException("Macro-action cannot be executed");
    }

    @Override
    public abstract List<Action> expand(Unit u) throws Exception;
}
