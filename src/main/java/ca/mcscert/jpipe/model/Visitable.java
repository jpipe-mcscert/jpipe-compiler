package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.visitors.ModelVisitor;

/**
 * Interface to annotate model element that can be visited by a Visitor.
 */
public interface Visitable {

    void accept(ModelVisitor<?> visitor);

}
