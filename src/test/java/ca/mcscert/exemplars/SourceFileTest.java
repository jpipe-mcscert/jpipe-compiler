package ca.mcscert.exemplars;

import static org.junit.jupiter.api.Assertions.fail;

import ca.mcscert.jpipe.compiler.CompilerFactory;
import ca.mcscert.jpipe.compiler.model.Transformation;
import ca.mcscert.jpipe.model.Unit;
import ca.mcscert.jpipe.model.elements.JustificationElement;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;

public abstract class SourceFileTest {

    // the unit obtained from the file returned by calling source();
    protected Unit unit;

    protected abstract String source();

    @BeforeEach
    public final void initialize() throws URISyntaxException {
        Transformation<InputStream, Unit> loader = CompilerFactory.loader();
        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        Path location = Path.of(ClassLoader.getSystemResource(source()).toURI());
        try (InputStream is = classloader.getResourceAsStream(source())) {
            this.unit = loader.fire(is, location.toString());
        } catch (IOException e) {
            fail("IOException thrown while loading " + source());
        }
    }


    protected final Set<String> asIdentifiers(Set<JustificationElement> elements) {
        return elements.stream().map(JustificationElement::getIdentifier)
                .collect(Collectors.toSet());
    }

}
