package ca.mcscert.jpipe.operators;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.model.elements.Pattern;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Merge operator.
 */
public class MergeOperator extends CompositionOperator {

    private static final Logger logger = LogManager.getLogger();

    @SuppressWarnings({"checkstyle:LeftCurly", "checkstyle:RightCurly"})
    @Override public String name() { return "merge"; }

    @Override
    protected boolean checkParameters(Map<String, String> params) {
        return params.containsKey("equivalence") && params.containsKey("threshold");
    }

    @Override
    protected boolean checkInputs(List<JustificationModel> inputs) {
        for (JustificationModel justification : inputs) {
            if (Pattern.class.isAssignableFrom(justification.getClass())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void execute(JustificationModel output, List<JustificationModel> inputs,
                           Map<String, String> params) {
        logger.info("MERGE on {}({})", inputs, params);

        int threshold = Integer.parseInt(params.get("threshold"));

        // Flatten all JustificationElement
        List<JustificationElement> justificationElementList = new LinkedList<>();
        for (JustificationModel justification : inputs) {
            justificationElementList.addAll(justification.contents());
        }
        JustificationElement[] representatives =
                new JustificationElement[justificationElementList.size()]; // Change to list
        Arrays.fill(representatives, null);

        // Group similar elements
        List<List<JustificationElement>> groups = new LinkedList<>(); // Change to set
        for (int i = 0; i < justificationElementList.size(); i++) {
            JustificationElement justificationElement = justificationElementList.get(i);
            List<JustificationElement> group = new LinkedList<>();
            if (representatives[i] != null) {
                continue;
            }
            group.add(justificationElement);
            representatives[i] = justificationElement;
            for (int j = i + 1; j < justificationElementList.size(); j++) {
                JustificationElement otherJustificationElement = justificationElementList.get(j);
                if (representatives[j] == null
                        && areSimilar(justificationElement.getLabel(),
                        otherJustificationElement.getLabel(), threshold)) {
                    group.add(otherJustificationElement);
                    representatives[j] = justificationElement;
                }
            }
            groups.add(group);
        }

        // Merge groups
        for (List<JustificationElement> group : groups) {
            JustificationElement first = group.getFirst();
            output.add(first);
        }
        // Check the supporting elements of new element
        // Find representative element
        // rep. ele. supports the rep of the new element
        for (List<JustificationElement> group : groups) {
            for (JustificationElement justificationElement : group) {
                if (!output.contents().contains(justificationElement)) {
                    JustificationElement first = group.getFirst();
                    for (JustificationElement sup : justificationElement.getSupports()) {
                        if (output.contents().contains(sup)) {
                            // newSup supports the representative element in the output
                            sup.supports(first);
                        }

                    }
                } else {
                    for (JustificationElement sup : justificationElement.getSupports()) {
                        if (!output.contents().contains(sup)) {
                            JustificationElement representativeElement =
                                    getJustificationElement(sup, justificationElementList,
                                            representatives);
                            justificationElement.removeSupport(sup);
                            representativeElement.supports(justificationElement);
                        }
                    }

                }
            }
        }
    }

    private static JustificationElement
        getJustificationElement(JustificationElement sup,
                                    List<JustificationElement> justificationElementList,
                                        JustificationElement[] representatives) {
        for (int i = 0; i < justificationElementList.size(); i++) {
            if (justificationElementList.get(i).equals(sup)) {
                return representatives[i];
            }
        }
        throw new RuntimeException("JustificationElement not found");
    }

    private boolean areSimilar(String s1, String s2, int threshold) {
        LevenshteinDistance levenshteinDistance = LevenshteinDistance.getDefaultInstance();
        int distance = levenshteinDistance.apply(s1, s2);
        double distancePercentage = s1.length() > s2.length()
                ? (double) distance / (double) s1.length() :
                (double) distance / (double) s2.length();
        return distancePercentage <= ((double) threshold / 100);

    }

}
