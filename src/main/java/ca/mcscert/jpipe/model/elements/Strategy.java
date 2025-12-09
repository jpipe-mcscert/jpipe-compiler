package ca.mcscert.jpipe.model.elements;

import ca.mcscert.jpipe.visitors.ModelVisitor;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Model what a Strategy is inside a Justification Model.
 */
public final class Strategy extends JustificationElement {

    private final Set<Support> supports;

    /**
     * A strategy has a unique identifier and a label.
     *
     * @param identifier the strategy's unique identified (inside this justification).
     * @param label the strategy's label.
     */
    public Strategy(String identifier, String label) {
        super(identifier, label);
        this.supports = new HashSet<>();
    }

    @Override
    public Set<JustificationElement> getSupports() {
        return new HashSet<>(supports);
    }

    @Override
    public Strategy shallow() {
        return new Strategy(this.identifier, this.label);
    }

    @Override
    public void supports(JustificationElement that) {
        that.acceptAsSupport(this);
    }

    @Override
    public void removeSupport(JustificationElement that) {
        removeAsSupportIfExisting(that.getIdentifier());
    }

    @Override
    protected void acceptAsSupport(Evidence e) {
        removeAsSupportIfExisting(e.getIdentifier());
        this.supports.add(e);
    }

    @Override
    protected void acceptAsSupport(SubConclusion sc) {
        removeAsSupportIfExisting(sc.getIdentifier());
        this.supports.add(sc);
    }

    @Override
    protected void acceptAsSupport(AbstractSupport as) {
        removeAsSupportIfExisting(as.getIdentifier());
        this.supports.add(as);
    }

    @Override
    public void accept(ModelVisitor<?> visitor) {
        visitor.visit(this);
    }

    private void removeAsSupportIfExisting(String identifier) {
        this.supports.removeIf(support -> support.getIdentifier().equals(identifier));
    }

}
