package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.error.SemanticError;
import java.util.HashMap;
import java.util.Map;

/**
 * Register turn-key instances of composition operators.
 */
public class OperatorRegister {

    private final Map<String, CompositionOperator> operators;
    private static OperatorRegister instance;

    private OperatorRegister() {
        this.operators = new HashMap<>();
        initialize();
    }

    private void initialize() {
        this.operators.put("merge", new MergeOperator());
        this.operators.put("assemble", new AssembleOperator());
        this.operators.put("refine", new RefinementOperator());
    }

    /**
     * Singleton pattern fot the operator register.
     *
     * @return the singleton-ed instance of OperatorRegister
     */
    public static OperatorRegister getInstance() {
        if (instance == null) {
            instance = new OperatorRegister();
        }
        return instance;
    }

    /**
     * Find a composition operator based on its name.
     *
     * @param name the name of the operator (to be used in the DSL).
     * @return the instance of the operator.
     */
    public CompositionOperator get(String name) {
        if (this.operators.containsKey(name)) {
            return this.operators.get(name);
        }
        throw new SemanticError("Unknown composition operator: " + name);
    }

}
