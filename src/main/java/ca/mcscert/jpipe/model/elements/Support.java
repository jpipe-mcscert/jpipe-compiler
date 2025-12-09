package ca.mcscert.jpipe.model.elements;

import java.io.Serializable;

/**
 * Define a common abstraction for element that can support others inside a jPipe Model.
 */
public abstract class Support extends JustificationElement {

    /**
     * A support binds an identifier to a label.
     *
     * @param identifier the support's unique identifier.
     * @param label the support's label.
     */
    public Support(String identifier, String label) {
        super(identifier, label);
    }

}
