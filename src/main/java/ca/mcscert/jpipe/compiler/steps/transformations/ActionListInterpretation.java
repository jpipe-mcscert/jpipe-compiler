package ca.mcscert.jpipe.compiler.steps.transformations;

import ca.mcscert.jpipe.actions.Action;
import ca.mcscert.jpipe.actions.ExecutionEngine;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.Unit;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Compilation step that executes a list of action on a _new_ unit.
 */
public final class ActionListInterpretation extends Transformation<List<Action>, Unit> {

    private static final Logger logger = LogManager.getLogger();

    /**
     * Execute the actions provided as input in a newly created unit, returned.
     *
     * @param input the list of actions to interpret.
     * @param source the source file name (for error mgmt purpose)
     * @return the unit, after having applied the actions.
     * @throws Exception is something goes wrong while executing the actions.
     */
    @Override
    protected Unit run(List<Action> input, String source) throws Exception {
        ExecutionEngine engine = new ExecutionEngine();
        return engine.spawn(source, input);
    }

}
