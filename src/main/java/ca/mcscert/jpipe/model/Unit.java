package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.model.elements.JustificationElement;
import ca.mcscert.jpipe.model.elements.JustificationModel;
import ca.mcscert.jpipe.visitors.ModelVisitor;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Define what a compilation Unit is in JPipe.
 */
public final class Unit implements Visitable {

    private final String name;
    private final SymbolTable<JustificationModel> contents;
    private final Path location;
    private final Set<String> loaded;

    /**
     * Instantiates a compilation unit, using its name (source file).
     *
     * @param name the source file used to run the compiler.
     * @param filePath path of the original file;
     */
    public Unit(String name, Path filePath) {
        this.name = name;
        this.contents = new SymbolTable<>();
        this.loaded = new HashSet<>();
        this.location = filePath;
        addLoadedFile(filePath);
    }

    public String getName() {
        return name;
    }


    /**
     * Check if a given path is already loaded in the current compilation unit.
     *
     * @param p the path to check.
     * @return true is already loaded, false elsewhere.
     */
    public boolean isInScope(Path p) {
        return this.loaded.contains(p.normalize().toAbsolutePath().toString());
    }

    /**
     * Add a path as part of the files loaded to contribute to this unit.
     *
     * @param p the path to add
     */
    public void addLoadedFile(Path p) {
        this.loaded.add(p.normalize().toAbsolutePath().toString());
    }

    /**
     * Record a new justification inside the unit symbol table.
     *
     * @param j the justification to record.
     */
    public void add(JustificationModel j) {
        this.contents.record(j.getName(), j);
    }

    /**
     * Delete a justification inside the unit symbol table.
     *
     * @param j the justification to delete.
     */
    public void remove(JustificationModel j) {
        this.contents.delete(j.getName());
    }

    /**
     * Add a justification element inside a justification.
     *
     * @param container the justification to contain the element.
     * @param e the element to add.
     */
    public void addInto(String container, JustificationElement e) {
        JustificationModel justification = contents.get(container);
        justification.add(e);
    }

    public JustificationModel get(String identifier) {
        return this.contents.get(identifier);
    }

    public boolean exists(String identifier) {
        return this.contents.keys().contains(identifier);
    }

    public Collection<JustificationModel> getContents() {
        return this.contents.values();
    }


    @Override
    public void accept(ModelVisitor<?> visitor) {
        visitor.visit(this);
    }

    @Override
    public String toString() {
        return "Unit{"
                + "name='" + name + '\''
                + ", contents=" + contents
                + ", loaded=" + loaded
                + '}';
    }
}
