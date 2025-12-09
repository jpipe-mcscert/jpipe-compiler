package ca.mcscert.jpipe.visitors.exporters;

import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.visitors.ModelVisitor;

/**
 * Export a flat (no pattern extension or composition rule) version of a given model.
 */
public class JpipeExporter extends ModelVisitor<StringBuilder> {

    public JpipeExporter() {
        super(new StringBuilder());
    }

    @Override
    public void visit(Conclusion c) {
        exportJustificationElement("conclusion", c);
    }

    @Override
    public void visit(Evidence e) {
        exportJustificationElement("evidence", e);
    }

    @Override
    public void visit(Strategy s) {
        exportJustificationElement("strategy", s);
    }

    @Override
    public void visit(SubConclusion sc) {
        exportJustificationElement("sub-conclusion", sc);
    }

    @Override
    public void visit(AbstractSupport as) {
        exportJustificationElement("@support", as);
    }

    @Override
    public void visit(Justification j) {
        exportJustificationModel("justification", j);
    }

    @Override
    public void visit(Pattern p) {
        exportJustificationModel("pattern", p);
    }

    @Override
    public void visit(Unit u) {
        throw new RuntimeException("Cannot export to graphviz an entire compilation unit!");
    }

    private void exportJustificationElement(String keyword, JustificationElement e) {
        String tpl = "%s %s is \"%s\"";
        this.accumulator
                .append("  ")
                .append(String.format(tpl, keyword, e.getIdentifier(), e.getLabel()))
                .append(System.lineSeparator());
    }

    private void exportSupports(JustificationElement e) {
        for (JustificationElement je : e.getSupports()) {
            String lnk = "%s supports %s";
            this.accumulator.append("  ")
                    .append(String.format(lnk, je.getIdentifier(), e.getIdentifier()))
                    .append(System.lineSeparator());
        }
    }


    private void exportJustificationModel(String keyword, JustificationModel model) {
        String tpl = "%s %s {";
        this.accumulator
                .append(String.format(tpl, keyword, model.getName()))
                .append(System.lineSeparator());
        // Exporting nodes
        for (JustificationElement je : model.contents()) {
            je.accept(this);
        }
        // Exporting relations
        for (JustificationElement je : model.contents()) {
            exportSupports(je);
        }
        this.accumulator.append("}").append(System.lineSeparator());
    }

}
