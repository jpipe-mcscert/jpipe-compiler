package ca.mcscert.jpipe.model.elements;

import ca.mcscert.jpipe.visitors.ModelVisitor;

/**
 * Define what a justification pattern is.
 */
public final class Pattern extends JustificationModel {


    /**
     * Creates a pattern based on its name.
     *
     * @param name the name (identifier) of the pattern
     */
    public Pattern(String name) {
        super(name);
    }

    private Pattern(String name, boolean ready) {
        super(name, ready);
    }

    @Override
    public void accept(ModelVisitor<?> visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer("Pattern {");
        sb.append(super.toString());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public Pattern replicate() {
        Pattern clone = new Pattern(this.name, this.frozen);
        deepLink(clone);
        return clone;
    }
}
