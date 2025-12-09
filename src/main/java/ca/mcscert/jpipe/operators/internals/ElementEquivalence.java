package ca.mcscert.jpipe.operators.internals;

import ca.mcscert.jpipe.model.elements.JustificationElement;

/**
 * Implement an equivalence relation between justification elements.
 * Elements are considered equivalent if they have the same type, and the same label.
 * TODO take logical connector as strategy into account (e.g., AND)
 */
public class ElementEquivalence
        implements BinaryOperator<JustificationElement, JustificationElement, Boolean> {

    @Override
    public Boolean apply(JustificationElement left, JustificationElement right) {
        return left.getLabel().equals(right.getLabel())
                && left.getClass().equals(right.getClass());
    }

}
