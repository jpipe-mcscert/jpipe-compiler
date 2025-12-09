package ca.mcscert.jpipe.model.elements;

import ca.mcscert.jpipe.visitors.ModelVisitor;
import java.util.Set;

/**
 * Model what a Conclusion is in the Model.
 */
public final class Conclusion extends JustificationElement {

    private Strategy strategy;

    /**
     * A conclusion bounds a label to an identifier.
     *
     * @param identifier the unique identifier of the element.
     * @param label the label of the conclusion.
     */
    public Conclusion(String identifier, String label) {
        super(identifier, label);
    }

    public Strategy getStrategy() {
        return strategy;
    }

    @Override
    public void supports(JustificationElement that) {
        that.acceptAsSupport(this);
    }

    @Override
    public void removeSupport(JustificationElement that) {
        this.strategy = null;
    }

    @Override
    public Set<JustificationElement> getSupports() {
        return (this.strategy == null ? Set.of() : Set.of(this.strategy));
    }

    @Override
    protected void acceptAsSupport(Strategy s) {
        this.strategy = s;
    }

    @Override
    public Conclusion shallow() {
        return new Conclusion(this.identifier, this.label);
    }

    @Override
    public void accept(ModelVisitor<?> visitor) {
        visitor.visit(this);
    }

    /**
     * This method creates a new {@code SubConclusion} instance
     * using the current conclusion's identifier and label.
     *
     * @param strategy the {@code Strategy} that the generated {@code SubConclusion} will support
     * @return a new {@code SubConclusion} instance derived from this {@code Conclusion}
     */
    public SubConclusion intoSubConclusion(Strategy strategy) {
        SubConclusion subConclusion = new SubConclusion(this.identifier, this.label);
        if (strategy != null) {
            subConclusion.supports(strategy);
        }
        subConclusion.acceptAsSupport(this.strategy);
        return subConclusion;
    }

    /**
     * This method creates a new {@code SubConclusion} instance
     * using the current conclusion's identifier and label.
     *
     * @return a new {@code SubConclusion} instance derived from this {@code Conclusion}
     */
    public SubConclusion intoSubConclusion() {
        SubConclusion subConclusion = new SubConclusion(this.identifier, this.label);
        subConclusion.acceptAsSupport(this.strategy);
        return subConclusion;
    }
}
