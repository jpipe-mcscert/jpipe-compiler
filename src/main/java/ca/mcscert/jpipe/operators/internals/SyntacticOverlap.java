package ca.mcscert.jpipe.operators.internals;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Computes the syntactic overlap between two justification model.
 * The overlap is defined as the set of symbols sharing the same identifiers in both. This is useful
 * when extending a pattern, to redefine some abstract elements into more concrete ones.
 */
public class SyntacticOverlap
        implements BinaryOperator<JustificationModel, JustificationModel, Set<String>> {

    @Override
    public Set<String> apply(JustificationModel left, JustificationModel right) {
        Set<String> leftSymbols = left.contents().stream()
                                        .map(JustificationElement::getIdentifier)
                                        .collect(Collectors.toSet());
        Set<String> rightSymbols = right.contents().stream()
                                        .map(JustificationElement::getIdentifier)
                                        .collect(Collectors.toSet());
        Set<String> result = new HashSet<>(leftSymbols);
        result.retainAll(rightSymbols);
        return result;
    }
}
