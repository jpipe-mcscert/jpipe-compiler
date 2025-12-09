package ca.mcscert.jpipe.model.elements;

import ca.mcscert.jpipe.visitors.ModelVisitor;
import java.util.Set;

/**
 * Model what an Evidence is in a Justification.
 */
public final class Evidence extends Support {

    /**
     * Instantiate an Evidence.
     *
     * @param identifier the evidence's identifier.
     * @param label its label.
     */
    public Evidence(String identifier, String label) {
        super(identifier, label);
    }

    @Override
    public Evidence shallow()  {
        return new Evidence(this.identifier, this.label);
    }

    @Override
    public void supports(JustificationElement that) {
        that.acceptAsSupport(this);
    }

    @Override
    public void removeSupport(JustificationElement that) {
        throw new IllegalArgumentException("Justification element "
                + this + " should not be supported by " + that);
    }

    @Override
    public void accept(ModelVisitor<?> visitor) {
        visitor.visit(this);
    }


    @Override
    public Set<JustificationElement> getSupports() {
        return Set.of();
    }
}
