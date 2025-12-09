package ca.mcscert.jpipe.compiler.steps.io.sinks;

import ca.mcscert.jpipe.cli.Configuration;
import ca.mcscert.jpipe.cli.Format;
import ca.mcscert.jpipe.compiler.model.Sink;
import ca.mcscert.jpipe.error.ErrorManager;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.model.MutableGraph;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Compiler sink, rendering a GraphViz MutableGraph as an image (file or stdout).
 */
public final class GraphVizRenderer implements Sink<MutableGraph> {

    private final Format format;
    private static final Logger logger = LogManager.getLogger();

    /**
     * Render the graph into an image, according to the provided format.
     *
     * @param format the image file format (e.g., SVG, PNG)
     */
    public GraphVizRenderer(Format format) {
        this.format = format;
    }

    @Override
    public void pourInto(MutableGraph input, String sinkFile) throws IOException {

        guru.nidi.graphviz.engine.Format out = switch (this.format) {
            case PNG -> guru.nidi.graphviz.engine.Format.PNG;
            case SVG -> guru.nidi.graphviz.engine.Format.SVG;
            case DOT -> guru.nidi.graphviz.engine.Format.DOT;
            default -> throw new IllegalArgumentException("Cannot process format ["
                                                            + format
                                                            + "] with a Graphviz renderer");
        };

        if (sinkFile.equals(Configuration.STDOUT_PATH)) {
            runGraphviz(System.out, out, input);
        } else {
            try (FileOutputStream fos = new FileOutputStream(sinkFile)) {
                runGraphviz(fos, out, input);
            } catch (IOException ioe) {
                ErrorManager.getInstance().fatal(ioe);
            }
        }
    }


    private void runGraphviz(OutputStream outStream, guru.nidi.graphviz.engine.Format outFormat,
                             MutableGraph graph) throws IOException {
        Graphviz.fromGraph(graph).render(outFormat).toOutputStream(outStream);
    }

}
