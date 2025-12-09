package ca.mcscert.jpipe.operators.internals;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Compute equivalence classes of elements between two justification models.
 * Assumption: there are no equivalent elements inside a given justification model, e.g., for a
 * given justification model, all elements are distinct according to the equivalence relation.
 */
public class EquivalenceClass
        implements BinaryOperator<JustificationModel, JustificationModel, Map<String, String>> {

    private final ElementEquivalence eq = new ElementEquivalence();

    @Override
    public Map<String, String> apply(JustificationModel left, JustificationModel right) {
        Map<String, String> result = new HashMap<>();
        Set<String> rightEquiv = new HashSet<>();
        for (JustificationElement leftElem : left.contents()) {
            for (JustificationElement rightElem : right.contents()) {
                if (! rightEquiv.contains(rightElem.getIdentifier())) {
                    if (eq.apply(leftElem, rightElem)) {
                        rightEquiv.add(rightElem.getIdentifier());
                        result.put(leftElem.getIdentifier(), rightElem.getIdentifier());
                    }
                }
            }
        }
        return result;
    }
}
