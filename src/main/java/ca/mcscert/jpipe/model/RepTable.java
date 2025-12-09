package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.error.DuplicateSymbol;
import ca.mcscert.jpipe.error.UnknownSymbol;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * The {@code RepTable} class represents a simple symbolic mapping
 * structure to track parent-child relationships
 * between elements of a generic type {@code T}.
 * It can be used to build and manage a hierarchy of symbols,
 * where each element may optionally have a single parent.
 *
 * @param <T> the type of the elements managed by this representation table
 */

public final class RepTable<T> {

    private final HashMap<T, T> table;

    public RepTable() {
        table = new HashMap<>();
    }

    public boolean containsKey(T key) {
        return table.containsKey(key);
    }

    /**
     * Records a new element and its parent in the representation table.
     *
     * @param current the element to record
     * @param parent the parent of the element
     * @throws DuplicateSymbol if the element is already recorded in the table
     */
    public void record(T current, T parent) {
        if (table.containsKey(current)) {
            throw new DuplicateSymbol(current.toString());
        } else {
            table.put(current, parent);
        }
    }

    /**
     * Records a new root element (i.e., with no parent) in the representation table.
     *
     * @param current the element to record
     * @throws DuplicateSymbol if the element is already recorded in the table
     */
    public void record(T current) {
        if (table.containsKey(current)) {
            throw new DuplicateSymbol(current.toString());
        } else {
            table.put(current, null);
        }
    }

    /**
     * Deletes an element from the representation table and safely reassigns any of its children.
     *
     * @param current the element to delete
     * @throws UnknownSymbol if the element does not exist in the table
     */
    public void delete(T current) {
        if (!table.containsKey(current)) {
            throw new UnknownSymbol(current.toString());
        }
        safeDelete(current);
    }

    /**
     * Retrieves the direct parent of the given element from the table.
     *
     * @param current the element whose parent is to be retrieved
     * @return the parent of the element, or {@code null} if it has no parent
     * @throws UnknownSymbol if the element does not exist in the table
     */
    public T getParent(T current) {
        if (table.containsKey(current)) {
            return table.get(current);
        }
        throw new UnknownSymbol(current.toString());
    }

    public T getOriginalParent(T current) {
        return recursiveGetParent(current, new HashSet<>());
    }

    private T recursiveGetParent(T current, Set<T> taken) {

        if (!table.containsKey(current)) {
            throw new UnknownSymbol(current.toString());
        }
        if (taken.contains(current)) {
            throw new UnknownSymbol(current.toString());
        }

        T parentValue = table.get(current);
        if (parentValue == null) {
            return current;
        }
        taken.add(current);
        return recursiveGetParent(parentValue, taken);

    }

    /**
     * Safely deletes the specified element from the representation table.
     *
     * @param current the element to safely delete
     * @throws UnknownSymbol if the element does not exist in the table
     */
    public void safeDelete(T current) {
        if (!table.containsKey(current)) {
            throw new UnknownSymbol(current.toString());
        }

        // Update any children whose parent is the current element
        for (T key : table.keySet()) {
            if (current.equals(table.get(key))) {
                table.put(key, null);  // Promote child to root (no parent)
            }
        }

        // Finally, remove the element
        table.remove(current);
    }

    public HashMap<T, T> getTable() {
        return table;
    }




}
