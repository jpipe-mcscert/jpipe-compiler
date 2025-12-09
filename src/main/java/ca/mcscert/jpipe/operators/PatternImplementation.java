package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.operators.internals.BinaryOperator;
import ca.mcscert.jpipe.operators.internals.SyntacticOverlap;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Create a justification model with Pattern x Model -> Model.
 */
public class PatternImplementation extends CompositionOperator {

    private final SyntacticOverlap syntaxOverlap = new SyntacticOverlap();

    @SuppressWarnings({"checkstyle:LeftCurly", "checkstyle:RightCurly"})
    @Override public String name() { return "implements"; }

    /**
     * The pattern implementation operator does nopt accept any additional parameters.
     *
     * @param params a set of key-value pairs to configure the operator.
     * @return true if no parameters are provided, false elsewhere
     */
    @Override
    protected boolean checkParameters(Map<String, String> params) {
        return params.isEmpty();
    }

    /**
     * Should take only 2 inputs: (1) a justification model and (2) a pattern.
     *
     * @param inputs a set of justification models to be used
     * @return true if the inputs are ok, false elsewhere
     */
    @Override
    protected boolean checkInputs(List<JustificationModel> inputs) {
        if (inputs.size() != 2) {
            return false;
        }
        JustificationModel pattern = inputs.get(1);
        return Pattern.class.isAssignableFrom(pattern.getClass());
    }

    @Override
    protected void execute(JustificationModel output,
                           List<JustificationModel> inputs, Map<String, String> params) {

        JustificationModel input = inputs.getFirst();
        Pattern pattern = (Pattern) inputs.get(1);

        // Dumping input into output
        for (JustificationElement e : input.contents()) {
            output.add(e);
            input.remove(e);
        }

        // rewriting overlapping
        Collection<String> overlapping = syntaxOverlap.apply(output, pattern);
        for (String overlap : overlapping) {
            pattern.replace(pattern.get(overlap), output.get(overlap));
        }

        // Loading remaining elements
        for (JustificationElement patternElement : pattern.contents()) {
            if (overlapping.contains(patternElement.getIdentifier())) {
                continue;
            }
            output.add(patternElement);
        }

    }

}
