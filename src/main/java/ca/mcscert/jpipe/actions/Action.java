package ca.mcscert.jpipe.actions;

import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import java.util.function.Function;

/**
 * Define what an Action is. (Actions implement a command pattern for model creation).
 */
public interface Action {

    /**
     * Check if an action behaves as a macro-action, i.e., an action that acts as an action
     * generator.
     *
     * @return true if the action must be expanded, false elsewhere.
     */
    boolean isExpandable();

    /**
     * Can this action be executed right now or should it be delayed?.
     * Delayed actions are quite rare (e.g., linking to a concept that should be defined later on),
     * so we provide a default implementation
     *
     * @return a function returning true if the action can be executed, false elsewhere
     */
    default Function<Unit, Boolean> condition() {
        return (unit -> Boolean.TRUE);
    }

    /**
     * An action is executed on a Unit, which is modified as a result by its side effect.
     * Executing an action that is supposed to be expanded must end.
     *
     * @param context the unit to apply the action to.
     * @throws Exception is something goes wrong.
     */
    void execute(Unit context) throws Exception;

    /**
     * Refine a macro-action into its expected sequence of actions.
     * An action must returns true when isExpandable() is called for this expansion to be executed.
     *
     * @param u the unit targeted by the macro action
     * @return the list of actions to be exeecuted.
     * @throws Exception if something goes wrong.
     */
    List<Action> expand(Unit u) throws Exception;

}
