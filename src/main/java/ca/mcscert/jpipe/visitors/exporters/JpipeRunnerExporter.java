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
 * Export a given justification for implementation in jPipe runner.
 */
@SuppressWarnings({ "checkstyle:LeftCurly", "checkstyle:RightCurly",
                    "checkstyle:EmptyLineSeparator" })
public class JpipeRunnerExporter extends ModelVisitor<StringBuilder> {

    public JpipeRunnerExporter() { super(new StringBuilder()); }

    @Override public void visit(Conclusion c) { toMethodDeclaration(c); }
    @Override public void visit(Evidence e)   { toMethodDeclaration(e); }
    @Override public void visit(Strategy s)   { toMethodDeclaration(s); }
    @Override public void visit(SubConclusion sc) { /* do nothing */}

    @Override
    public void visit(AbstractSupport as) {
        throw new RuntimeException("Cannot export an abstract support to jpipe runner!");
    }

    @Override
    public void visit(Pattern p) {
        throw new RuntimeException("Cannot export a pattern to jpipe runner!");
    }

    @Override
    public void visit(Unit u) {
        throw new RuntimeException("Cannot export an entire compilation unit!");
    }

    @Override
    public void visit(Justification j) {
        toFileHeader(j);
        for (JustificationElement e : j.contents()) {
            e.accept(this);
        }
    }


    private String toSnakeCase(String s) {
        return s.toLowerCase().replace(" ", "_");
    }

    private void toMethodDeclaration(JustificationElement e) {
        // header
        this.accumulator.append("## ")
                .append(e.getClass().getSimpleName())
                .append(" ").append(e.getIdentifier()).append(System.lineSeparator());
        // method signature:
        this.accumulator.append("def ")
                .append(toSnakeCase(e.getLabel())).append("() -> bool:")
                .append(System.lineSeparator());
        // scaffolding body
        this.accumulator.append("    ").append("return True")
                .append(System.lineSeparator()).append(System.lineSeparator());
    }

    private void toFileHeader(JustificationModel m) {
        this.accumulator.append("######").append(System.lineSeparator())
                .append("## ").append(m.getClass().getSimpleName())
                .append(" ").append(m.getName()).append(System.lineSeparator())
                .append("######").append(System.lineSeparator()).append(System.lineSeparator());
    }

}
