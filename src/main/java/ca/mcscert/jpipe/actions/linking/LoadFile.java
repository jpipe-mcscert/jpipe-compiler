package ca.mcscert.jpipe.actions.linking;

import ca.mcscert.jpipe.actions.Action;
import ca.mcscert.jpipe.actions.MacroAction;
import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.compiler.model.Source;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.compiler.steps.io.sources.FileReader;
import ca.mcscert.jpipe.error.SemanticError;
import ca.mcscert.jpipe.model.Unit;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Load a file referenced in another file.
 */
public class LoadFile extends MacroAction {

    private static final Logger logger = LogManager.getLogger();

    private final String fileName;

    public LoadFile(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public List<Action> expand(Unit context) throws Exception {
        Path p = new File(fileName).toPath().normalize().toAbsolutePath();
        System.out.println(p);
        if (canLoad(p, context)) {

            context.addLoadedFile(p);
            Transformation<InputStream, List<Action>> partial = CompilerFactory.actionProvider();
            Source<InputStream> reader = new FileReader();
            try (InputStream in = reader.provideFrom(p.toString())) {
                return partial.fire(in, p.toString());
            }
        } else {
            return List.of(); // Nothing to do, file already loaded
        }
    }

    private boolean canLoad(Path p, Unit context) {
        if (!p.toFile().exists()) {
            throw new SemanticError("Cannot load file " + p + " (file not found or not readable)");
        }
        return (!context.isInScope(p)); // Load only if not already loaded;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("LoadFile{");
        sb.append("fileName='").append(fileName).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
