package ca.mcscert.jpipe.model;

import ca.mcscert.jpipe.error.DuplicateSymbol;
import ca.mcscert.jpipe.error.UnknownSymbol;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

/**
 * Define a scope inside a model element, binding local symbols (strings) to elements of type T.
 *
 * <p>Example: a given justification contains a symbol table of JustificationElements.</p>
 *
 * @param <T> the type of elements stored in the
 */
public final class SymbolTable<T> {

    private final Map<String, T> symbols;

    /**
     * Build an empty table.
     */
    public SymbolTable() {
        this.symbols = new HashMap<>();
    }

    /**
     * Record a new symbol in the symbol table.
     *
     * @param identifier the symbol's identifier.
     * @param element the element to record.
     * @throws DuplicateSymbol is this symbol is already used in the table.
     */
    public void record(String identifier, T element) throws DuplicateSymbol {
        if (this.keys().contains(identifier)) {
            throw new DuplicateSymbol(identifier);
        }
        this.symbols.put(identifier, element);
    }

    /**
     * Delete a symbol in the table.
     *
     * @param identifier the identifier to remove from the table
     * @throws UnknownSymbol if that very symbol does not exist
     */
    public void delete(String identifier) throws UnknownSymbol {
        if (!this.keys().contains(identifier)) {
            throw new UnknownSymbol(identifier);
        }
        this.symbols.remove(identifier);
    }


    /**
     * Get the element associated to a given symbol inside the table.
     *
     * @param identifier the identifier one is looking for.
     * @return the stored element, if any.
     * @throws UnknownSymbol if the symbol is not defined in the table.
     */
    public T get(String identifier) throws UnknownSymbol {
        if (this.symbols.containsKey(identifier)) {
            return this.symbols.get(identifier);
        }
        throw new UnknownSymbol(identifier);
    }

    /**
     * Return all the elements recorded inside the table.
     *
     * @return a collection of elements.
     */
    public Collection<T> values() {
        return new HashSet<>(this.symbols.values());
    }

    /**
     * Compute the set of keys defined in this symbol table.
     *
     * @return The symbols, as a set of strings.
     */
    public Set<String> keys() {
        return new HashSet<>(this.symbols.keySet());
    }

    /**
     * Check if a given symbol exists inside the table.
     *
     * @param identifier the symbol to look for.
     * @return true if it exists, false elsewhere.
     */
    public boolean exists(String identifier) {
        return this.symbols.containsKey(identifier);
    }


    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",", "{", "}");
        Set<String> keys = this.keys();
        for (String key : keys) {
            joiner.add(key + " -->> " + this.get(key).toString());
        }
        return joiner.toString();
    }

}
