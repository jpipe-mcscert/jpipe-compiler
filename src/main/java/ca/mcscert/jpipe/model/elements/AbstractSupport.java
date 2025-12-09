package ca.mcscert.jpipe.model.elements;


import ca.mcscert.jpipe.visitors.ModelVisitor;
import java.util.Set;

/**
 * Model what an abstract support is for justification patterns.
 */
public final class AbstractSupport extends Support {

    public AbstractSupport(String identifier, String label) {
        super(identifier, label);
    }

    @Override
    public void supports(JustificationElement that) {
        that.acceptAsSupport(this);
    }

    @Override
    public void removeSupport(JustificationElement that) {
        that.removeSupport(this);
    }

    @Override
    public AbstractSupport shallow() {
        return new AbstractSupport(this.identifier, this.label);
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
