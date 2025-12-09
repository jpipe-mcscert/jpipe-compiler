package ca.mcscert.jpipe.model.elements;

import ca.mcscert.jpipe.model.RepTable;
import ca.mcscert.jpipe.model.SymbolTable;
import ca.mcscert.jpipe.model.Visitable;
import ca.mcscert.jpipe.model.cloning.Replicable;
import java.util.Collection;
import java.util.Map;

/**
 * Abstraction to represent patters and justification in a uniform way.
 */
public abstract class JustificationModel
        implements Visitable, Replicable<JustificationModel> {

    protected final String name;
    protected final SymbolTable<JustificationElement> symbols;
    protected RepTable<JustificationElement> repTable;
    protected boolean frozen;

    protected JustificationModel(String name, boolean isFrozen) {
        this.name = name;
        this.symbols = new SymbolTable<>();
        this.repTable = new RepTable<>();
        this.frozen = isFrozen;
    }

    protected JustificationModel(String name) {
        this(name, false);
    }

    public String getName() {
        return name;
    }

    public void freeze() {
        this.frozen = true;
    }

    public void unfreeze() {
        this.frozen = false;
    }

    public boolean isFrozen() {
        return this.frozen;
    }

    public JustificationElement get(String identifier) {
        String id = (identifier.startsWith(this.name) ? identifier.split(":")[1] : identifier);
        return this.symbols.get(id);
    }

    public Collection<JustificationElement> contents() {
        return this.symbols.values();
    }

    public Map<JustificationElement, JustificationElement> representations() {
        return this.repTable.getTable();
    }

    /**
     * Add an element inside an unlocked justification.
     *
     * @param e the element to add
     */
    public void add(JustificationElement e) {
        if (this.isFrozen()) {
            throw new IllegalStateException("Cannot add an element to a frozen justification");
        }
        this.symbols.record(e.getIdentifier(), e);
        this.repTable.record(e);
        e.setContainer(this);
    }

    /**
     * Add an element inside an unlocked justification with the representative element.
     *
     * @param e the element to add
     * @param rep the representative element of e
     */
    public void add(JustificationElement e, JustificationElement rep) {
        if (this.isFrozen()) {
            throw new IllegalStateException("Cannot add an element to a frozen justification");
        }
        this.symbols.record(e.getIdentifier(), e);
        this.repTable.record(e, rep);
        e.setContainer(this);

    }

    /**
     * Remove an element from a given justification.
     *
     * @param e the element to remove.
     */
    public void remove(JustificationElement e) {
        if (this.isFrozen()) {
            throw new IllegalStateException("Cannot remove an element from a frozen justification");
        }
        if (! this.symbols.exists(e.getIdentifier())) {
            String msg = "Cannot remove a non-existing element from a justification";
            throw new IllegalStateException(msg);
        }
        e.setContainer(null);
        this.symbols.delete(e.getIdentifier());
    }


    /**
     * Replace an element by another one inside a justification.
     *
     * @param original the element to replace.
     * @param newOne the element replacing the original.
     */
    public void replace(JustificationElement original, JustificationElement newOne) {
        // Re-wire things in the justification model body.
        for (JustificationElement elem : this.contents()) {
            for (JustificationElement supporting : elem.getSupports()) {
                if (supporting.getIdentifier().equals(original.getIdentifier())) {
                    newOne.supports(elem);
                }
            }
        }
        // Delete the original, and add the new one.
        this.remove(original);
        this.add(newOne);
    }

    /**
     * Method called in subclasses to implement the justification model replication (deep clone).
     * Taking an empty shell as parameter, it fills the shell with all the elements contained inside
     * this, and re-wire all relations between the newly cloned elements as they were in the
     * original object.
     *
     * @param clone the justification model to modify (side effect) with the deep-cloning process.
     */
    protected final void deepLink(JustificationModel clone) {
        clone.unfreeze(); // opening the clone for modification

        // Building shallow clone of each element in the justification model
        for (String id : this.symbols.keys()) {
            JustificationElement elem = this.get(id).shallow();
            repTable.record(elem, this.get(id));
            clone.add(elem); // replacing by the cloned (but still not wired) one.
        }

        // Translating the relation existing in 'this' into 'clone'.
        for (String id : clone.symbols.keys()) {
            JustificationElement cloned = clone.get(id);
            for (JustificationElement supporting : this.get(id).getSupports()) {
                JustificationElement supportingClone = clone.get(supporting.getIdentifier());
                supportingClone.supports(cloned);
            }
        }
        clone.repTable = this.repTable;
    }


    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("JustificationModel{");
        sb.append("name='").append(name).append('\'');
        sb.append(", symbols=").append(symbols);
        sb.append(", ready=").append(frozen);
        sb.append('}');
        return sb.toString();
    }

    public JustificationElement getDirectReplacementOf(JustificationElement elem) {
        return repTable.getParent(elem);
    }

    public JustificationElement getOriginalOf(JustificationElement elem) {
        return repTable.getOriginalParent(elem);
    }

    public boolean hasReplacementRecord(JustificationElement elem) {
        return repTable.containsKey(elem);
    }

}
