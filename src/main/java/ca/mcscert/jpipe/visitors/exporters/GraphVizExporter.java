package ca.mcscert.jpipe.visitors.exporters;

import static guru.nidi.graphviz.model.Factory.mutGraph;
import static guru.nidi.graphviz.model.Factory.mutNode;
import static guru.nidi.graphviz.model.Factory.node;

import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.AbstractSupport;
import ca.mcscert.jpipe.model.elements.Conclusion;
import ca.mcscert.jpipe.model.elements.Evidence;
import ca.mcscert.jpipe.model.elements.Justification;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.Pattern;
import ca.mcscert.jpipe.model.elements.Strategy;
import ca.mcscert.jpipe.model.elements.SubConclusion;
import ca.mcscert.jpipe.visitors.ModelVisitor;
import guru.nidi.graphviz.attribute.Attributes;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.MutableGraph;
import guru.nidi.graphviz.model.MutableNode;

/**
 * Visit a jPipe model to produce a GraphViz Mutable Graph (used for image rendering).
 */
public class GraphVizExporter extends ModelVisitor<MutableGraph> {

    private String context = "";

    /**
     * Creates a Mutable Graph exporter visitor. Instantiating the accumulator as a properly
     * configured mutable graph, modified while visiting.
     */
    public GraphVizExporter() {
        // Mutable graph (Graphviz data structure) as output
        super(mutGraph()
                .setDirected(true)
                .graphAttrs()
                .add(Rank.dir(Rank.RankDir.BOTTOM_TO_TOP)));
    }

    @Override
    public void visit(Unit u) {
        throw new RuntimeException("Cannot export to graphviz an entire compilation unit!");
    }

    @Override
    public void visit(Justification j) {
        this.context = j.getName();
        // Setting justification name
        this.accumulator.graphAttrs().add(Label.markdown(j.getName()));
        for (JustificationElement je : j.contents()) {
            je.accept(this);
        }
        this.context = "";
    }

    @Override
    public void visit(Pattern p) {
        this.context = p.getName();
        String label = "&lt;&lt;pattern&gt;&gt;&nbsp;" + p.getName();
        this.accumulator.graphAttrs().add(Label.html(label));
        for (JustificationElement je : p.contents()) {
            je.accept(this);
        }
        this.context = "";
    }

    @Override
    public void visit(Conclusion c) {
        MutableNode n = asMutableNode(c)
                .add(Label.markdown(c.getLabel()))
                .add(Shape.RECT)
                .add(Style.combine(Style.FILLED, Style.ROUNDED))
                .add(Color.LIGHTGREY.fill())
                .add(Style.lineWidth(1.01))
                ;
        n.addTo(this.accumulator);
        if (c.getStrategy() != null) {
            this.accumulator.add(node(c.getStrategy().getIdentifier()).link(n));
        }
    }

    @Override
    public void visit(Evidence e) {
        MutableNode n = asMutableNode(e)
                .add(Label.markdown(e.getLabel()))
                .add(Shape.NOTE)
                .add(Color.LIGHTSKYBLUE2.fill())
                .add(Style.FILLED)
                .add(Style.lineWidth(1.01));
        n.addTo(this.accumulator);
    }

    @Override
    public void visit(AbstractSupport as) {
        MutableNode n = asMutableNode(as)
                .add(Label.html("<i>" + as.getLabel() + "</i>"))
                .add(Shape.RECT)
                .add(Style.DOTTED)
                .add(Style.lineWidth(1.01));
        n.addTo(this.accumulator);
    }

    @Override
    public void visit(Strategy s) {
        MutableNode n = asMutableNode(s)
                .add(Label.markdown(s.getLabel()))
                .add(Shape.PARALLELOGRAM)
                .add(Style.FILLED)
                .add(Color.PALEGREEN.fill())
                .add(Style.lineWidth(1.01));
        n.addTo(this.accumulator);
        for (JustificationElement su : s.getSupports()) {
            this.accumulator.add(node(su.getIdentifier()).link(n));
        }
    }

    @Override
    public void visit(SubConclusion sc) {
        MutableNode n = asMutableNode(sc)
                .add(Label.markdown(sc.getLabel()))
                .add(Shape.RECT)
                .add(Color.DODGERBLUE)
                .add(Style.lineWidth(1.01));
        n.addTo(this.accumulator);
        if (sc.getStrategy() != null) {
            this.accumulator.add(node(sc.getStrategy().getIdentifier()).link(n));
        }
    }

    private MutableNode asMutableNode(JustificationElement e) {
        return mutNode(e.getIdentifier())
                .add(Attributes.attr("id", buildIdentifier(e)));
    }

    private String buildIdentifier(JustificationElement e) {
        return String.format("%s:%s", this.context, e.getIdentifier());
    }

}
