package ca.mcscert.jpipe.actions.linking;

import ca.mcscert.jpipe.actions.RegularAction;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.operators.CompositionOperator;
import ca.mcscert.jpipe.operators.OperatorRegister;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Call a composition operator.
 */
public class CallOperator extends RegularAction {

    private final CompositionOperator.ReturnType returnType;
    private final String outputModel;
    private final String operator;
    private final List<String> inputModels = new LinkedList<>();
    private final Map<String, String> parameters = new HashMap<>();

    /**
     * Create an operator call.
     *
     * @param type the return type of this call.
     * @param outputModel the name of the justification to be created.
     * @param operator the operator to call.
     */
    public CallOperator(CompositionOperator.ReturnType type, String outputModel, String operator) {
        this.returnType = type;
        this.outputModel = outputModel;
        this.operator = operator;
    }

    public void addInputModel(String model) {
        this.inputModels.add(model);
    }

    public void addParameter(String key, String value) {
        this.parameters.put(key, value);
    }

    @Override
    public Function<Unit, Boolean> condition() {
        return (u) -> this.inputModels.stream().map(u::exists).reduce(true, Boolean::logicalAnd);
    }

    @Override
    public void execute(Unit context) throws Exception {
        logger.trace("Calling composition operator {}", operator);
        CompositionOperator comp = OperatorRegister.getInstance().get(operator);
        List<JustificationModel> ins = inputModels.stream().map(context::get).toList();
        JustificationModel result = comp.run(returnType, outputModel, ins, parameters);
        context.add(result);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("CallOperator{");
        sb.append("returnType=").append(returnType);
        sb.append(", outputModel='").append(outputModel).append('\'');
        sb.append(", inputModels=").append(inputModels);
        sb.append(", operator='").append(operator).append('\'');
        sb.append(", parameters=").append(parameters);
        sb.append('}');
        return sb.toString();
    }
}
